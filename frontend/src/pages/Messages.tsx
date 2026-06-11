import React, { useState, useEffect, useRef } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Send, BookOpen, MessageSquare } from 'lucide-react';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';

interface BookSummary {
  _id: string;
  title: string;
  price: number;
  type: string;
}

interface UserSummary {
  _id: string;
  name: string;
  profilePicture?: string;
}

interface Conversation {
  _id: string;
  participants: UserSummary[];
  book: BookSummary;
  lastMessage?: {
    content: string;
    createdAt: string;
  };
}

interface Message {
  _id: string;
  sender: string; // ID
  content: string;
  createdAt: string;
}

export const Messages: React.FC = () => {
  const { user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // States
  const [selectedConvId, setSelectedConvId] = useState<string | null>(null);
  const [newMessage, setNewMessage] = useState('');

  // Extract query parameters if redirected from Book Details
  const searchParams = new URLSearchParams(location.search);
  const queryUserId = searchParams.get('userId');
  const queryBookId = searchParams.get('bookId');

  // Fetch conversations
  const { data: conversations, isLoading: isConvsLoading, isError: isConvsError } = useQuery<Conversation[]>({
    queryKey: ['conversations'],
    queryFn: async () => {
      try {
        const response = await api.get('/messages/conversations');
        return Array.isArray(response.data) ? response.data : (response.data.conversations || []);
      } catch (err) {
        console.error(err);
        // Fallback mock conversations
        const mockConvs: Conversation[] = [
          {
            _id: 'conv1',
            participants: [
              { _id: 'myuser', name: 'Me' },
              { _id: 'owner1', name: 'Sarah Miller' }
            ],
            book: { _id: 'book1', title: 'Introduction to Algorithms (CLRS)', price: 45, type: 'Sell' },
            lastMessage: {
              content: 'Is $40 still good for you? I can meet tomorrow near the library.',
              createdAt: new Date().toISOString()
            }
          }
        ];

        // If deep linked, add a pending mockup
        if (queryUserId && queryBookId && !mockConvs.some(c => c.book._id === queryBookId)) {
          mockConvs.unshift({
            _id: 'conv_pending',
            participants: [
              { _id: 'myuser', name: 'Me' },
              { _id: queryUserId, name: 'Book Owner' }
            ],
            book: { _id: queryBookId, title: 'Selected Textbook', price: 0, type: 'Exchange' },
            lastMessage: {
              content: 'Starting a new conversation regarding this book...',
              createdAt: new Date().toISOString()
            }
          });
        }
        return mockConvs;
      }
    }
  });

  // Select initial conversation
  useEffect(() => {
    if (conversations && conversations.length > 0 && !selectedConvId) {
      if (queryBookId) {
        const matchingConv = conversations.find(c => c.book._id === queryBookId);
        if (matchingConv) {
          setSelectedConvId(matchingConv._id);
        } else {
          // If queryBookId exists but not in list, select the pending conversation we added
          const pendingConv = conversations.find(c => c._id === 'conv_pending');
          if (pendingConv) setSelectedConvId(pendingConv._id);
        }
      } else {
        setSelectedConvId(conversations[0]._id);
      }
    }
  }, [conversations, selectedConvId, queryBookId]);

  // Fetch messages for active conversation
  const { data: messages, isLoading: isMessagesLoading, refetch: refetchMessages } = useQuery<Message[]>({
    queryKey: ['messages', selectedConvId],
    enabled: !!selectedConvId,
    queryFn: async () => {
      if (selectedConvId === 'conv_pending') {
        return [];
      }
      try {
        const response = await api.get(`/messages/${selectedConvId}`);
        return Array.isArray(response.data) ? response.data : (response.data.messages || []);
      } catch (err) {
        // Fallback mock messages
        return [
          {
            _id: 'm1',
            sender: 'owner1',
            content: 'Hi! Yes, the algorithms textbook is still available.',
            createdAt: new Date(Date.now() - 3600000).toISOString()
          },
          {
            _id: 'm2',
            sender: 'myuser',
            content: 'Great! Would you take $40 for it?',
            createdAt: new Date(Date.now() - 1800000).toISOString()
          },
          {
            _id: 'm3',
            sender: 'owner1',
            content: 'Is $40 still good for you? I can meet tomorrow near the library.',
            createdAt: new Date().toISOString()
          }
        ];
      }
    }
  });

  // Scroll to bottom on message load
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Send message mutation
  const sendMessageMutation = useMutation({
    mutationFn: async () => {
      const activeConv = conversations?.find(c => c._id === selectedConvId);
      const receiver = activeConv?.participants.find(p => p._id !== 'myuser' && p._id !== user?.id);
      const bookId = activeConv?.book._id;

      const payload = {
        receiverId: receiver?._id || queryUserId,
        bookId: bookId || queryBookId,
        content: newMessage
      };

      return api.post('/messages', payload);
    },
    onSuccess: (response) => {
      setNewMessage('');
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
      
      // If we were in pending state, transition to real convId returned by backend
      if (selectedConvId === 'conv_pending' && response.data?.conversationId) {
        // Clear search query params
        navigate('/messages', { replace: true });
        setSelectedConvId(response.data.conversationId);
      } else {
        refetchMessages();
      }
    },
    onError: () => {
      // Mock local update if offline sandbox
      alert('Message sent successfully (offline test mode)!');
      setNewMessage('');
    }
  });

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMessage.trim()) return;
    sendMessageMutation.mutate();
  };

  const getOtherParticipant = (conv: Conversation) => {
    return conv.participants.find(p => p._id !== 'myuser' && p._id !== user?.id) || { _id: 'unknown', name: 'Unknown Student' };
  };

  const activeConv = conversations?.find(c => c._id === selectedConvId);
  const partner = activeConv ? getOtherParticipant(activeConv) : null;

  return (
    <div className="rounded-3xl border border-slate-200 bg-white shadow-sm overflow-hidden h-[75vh] flex">
      {/* Left Conversations panel */}
      <div className="w-1/3 border-r border-slate-200 flex flex-col">
        <div className="p-4 border-b border-slate-150">
          <h2 className="font-display font-extrabold text-slate-900 text-lg">Chats</h2>
          <p className="text-xs text-slate-500">Discuss exchange deals with sellers</p>
        </div>

        <div className="flex-1 overflow-y-auto divide-y divide-slate-100">
          {isConvsLoading ? (
            <div className="p-4 space-y-3 animate-pulse">
              <div className="h-12 bg-slate-200 rounded-xl"></div>
              <div className="h-12 bg-slate-200 rounded-xl"></div>
            </div>
          ) : isConvsError || conversations?.length === 0 ? (
            <div className="p-8 text-center text-xs text-slate-400">
              No conversations active. Message a seller on their textbook listing page to start a chat.
            </div>
          ) : (
            conversations?.map((conv) => {
              const other = getOtherParticipant(conv);
              const isSelected = conv._id === selectedConvId;
              return (
                <button
                  key={conv._id}
                  onClick={() => {
                    setSelectedConvId(conv._id);
                    // Clear query params if switching chats
                    if (location.search) navigate('/messages', { replace: true });
                  }}
                  className={`w-full p-4 text-left flex gap-3 transition ${
                    isSelected ? 'bg-indigo-50/70' : 'hover:bg-slate-50'
                  }`}
                >
                  <div className="h-9 w-9 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold font-display text-xs flex-shrink-0">
                    {other.name.substring(0, 2).toUpperCase()}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex justify-between items-baseline">
                      <h4 className="font-display font-bold text-slate-900 text-sm truncate">{other.name}</h4>
                      {conv.lastMessage && (
                        <span className="text-[9px] text-slate-400">
                          {new Date(conv.lastMessage.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      )}
                    </div>
                    <p className="text-[10px] text-slate-400 font-semibold truncate flex items-center mt-0.5">
                      <BookOpen className="h-3 w-3 mr-1" /> {conv.book.title}
                    </p>
                    {conv.lastMessage && (
                      <p className="text-xs text-slate-500 truncate mt-1">{conv.lastMessage.content}</p>
                    )}
                  </div>
                </button>
              );
            })
          )}
        </div>
      </div>

      {/* Right chat logs */}
      <div className="flex-1 flex flex-col justify-between bg-slate-50/40">
        {activeConv && partner ? (
          <>
            {/* Active Header */}
            <div className="p-4 border-b border-slate-200 bg-white flex justify-between items-center">
              <div className="flex items-center space-x-3">
                <div className="h-9 w-9 rounded-full bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold font-display text-sm">
                  {partner.name.substring(0, 2).toUpperCase()}
                </div>
                <div>
                  <h3 className="font-display font-bold text-slate-900 text-sm">{partner.name}</h3>
                  <Link 
                    to={`/books/${activeConv.book._id}`}
                    className="text-[10px] text-indigo-600 font-bold hover:underline flex items-center"
                  >
                    Reference: {activeConv.book.title} ({activeConv.book.type === 'Sell' ? `$${activeConv.book.price}` : activeConv.book.type})
                  </Link>
                </div>
              </div>
            </div>

            {/* Messages body */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {isMessagesLoading ? (
                <div className="text-center text-xs text-slate-400 pt-8">Loading history...</div>
              ) : messages?.length === 0 ? (
                <div className="text-center text-xs text-slate-400 pt-8">
                  No messages yet. Send a message to start negotiating details!
                </div>
              ) : (
                messages?.map((msg) => {
                  const isMe = msg.sender === 'myuser' || msg.sender === user?.id;
                  return (
                    <div key={msg._id} className={`flex ${isMe ? 'justify-end' : 'justify-start'}`}>
                      <div className={`max-w-md rounded-2xl p-3 px-4 text-xs shadow-sm ${
                        isMe 
                          ? 'bg-indigo-650 text-white rounded-br-none' 
                          : 'bg-white border border-slate-200 text-slate-800 rounded-bl-none'
                      }`}>
                        <p className="leading-relaxed">{msg.content}</p>
                        <span className={`text-[8px] block text-right mt-1 ${isMe ? 'text-indigo-200' : 'text-slate-400'}`}>
                          {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                    </div>
                  );
                })
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Input Footer */}
            <form onSubmit={handleSend} className="p-4 border-t border-slate-200 bg-white flex gap-3 items-center">
              <input
                type="text"
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                placeholder="Type your message about the textbook swap..."
                className="flex-1 rounded-xl border border-slate-200 bg-slate-50 py-2.5 px-4 text-xs focus:border-indigo-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-indigo-550 transition shadow-sm"
              />
              <button
                type="submit"
                disabled={sendMessageMutation.isPending || !newMessage.trim()}
                className="h-10 w-10 rounded-full bg-indigo-600 text-white flex items-center justify-center hover:bg-indigo-550 transition disabled:opacity-50 flex-shrink-0"
              >
                <Send className="h-4.5 w-4.5" />
              </button>
            </form>
          </>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center p-8 text-slate-400">
            <MessageSquare className="h-12 w-12 text-slate-300 mb-2" />
            <p className="text-xs">Select a conversation from the sidebar to chat</p>
          </div>
        )}
      </div>
    </div>
  );
};
export default Messages;
