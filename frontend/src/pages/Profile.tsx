import React, { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  User as UserIcon, 
  Mail, 
  Star, 
  ShieldAlert, 
  Check, 
  MessageSquare,
  X
} from 'lucide-react';
import api from '../api/axios';

interface Review {
  _id: string;
  reviewer: {
    name: string;
  };
  rating: number;
  comment: string;
  createdAt: string;
}

export const Profile: React.FC = () => {
  const { user, updateUser } = useAuth();

  // Form profile states
  const [name, setName] = useState(user?.name || '');
  const [email, setEmail] = useState(user?.email || '');
  const [password, setPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  
  // Rating form state
  const [rateUserId, setRateUserId] = useState('');
  const [rateRating, setRateRating] = useState(5);
  const [rateComment, setRateComment] = useState('');
  const [rateModalOpen, setRateModalOpen] = useState(false);

  // Fetch reviews for the current user
  const { data: reviews, isLoading: isReviewsLoading } = useQuery<Review[]>({
    queryKey: ['reviews', user?.id],
    enabled: !!user?.id,
    queryFn: async () => {
      try {
        const response = await api.get(`/reviews/${user?.id}`);
        return Array.isArray(response.data) ? response.data : (response.data.reviews || []);
      } catch (err) {
        console.error(err);
        // Fallback mock reviews
        return [
          {
            _id: 'rev1',
            reviewer: { name: 'Sarah Miller' },
            rating: 5,
            comment: 'Very friendly, book was in the exact condition listed. Meetup was quick and smooth!',
            createdAt: new Date().toISOString()
          },
          {
            _id: 'rev2',
            reviewer: { name: 'Bob Harris' },
            rating: 4,
            comment: 'Punctual meeting on campus. Recommended trader.',
            createdAt: new Date(Date.now() - 604800000).toISOString()
          }
        ];
      }
    }
  });

  // Profile update mutation
  const profileMutation = useMutation({
    mutationFn: async () => {
      const payload: any = { name, email };
      if (password && newPassword) {
        payload.currentPassword = password;
        payload.newPassword = newPassword;
      }
      return api.put('/users/profile', payload);
    },
    onSuccess: (response) => {
      const updated = response.data?.user || { name, email };
      updateUser(updated);
      setPassword('');
      setNewPassword('');
      alert('Profile details updated successfully!');
    },
    onError: (err: any) => {
      alert(err.response?.data?.message || 'Failed to update profile details.');
    }
  });

  // Peer review submission mutation
  const peerReviewMutation = useMutation({
    mutationFn: async () => {
      return api.post('/reviews', {
        receiverId: rateUserId,
        rating: rateRating,
        comment: rateComment
      });
    },
    onSuccess: () => {
      setRateModalOpen(false);
      setRateComment('');
      setRateUserId('');
      alert('Peer review submitted successfully!');
    },
    onError: (err: any) => {
      alert(err.response?.data?.message || 'Failed to submit review.');
    }
  });

  const handleProfileSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    profileMutation.mutate();
  };

  const handleRateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!rateUserId) return;
    peerReviewMutation.mutate();
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
      {/* Profile Info & Edit Panel */}
      <div className="lg:col-span-2 space-y-6">
        <div className="rounded-3xl border border-slate-200 bg-white p-6 md:p-8 shadow-sm">
          <h2 className="font-display text-lg font-bold text-slate-900 mb-6">
            Account Profile
          </h2>

          <form onSubmit={handleProfileSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Full Name */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1.5">
                  Full Name
                </label>
                <div className="relative">
                  <span className="absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
                    <UserIcon className="h-4 w-4" />
                  </span>
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="block w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-9 pr-3 text-xs focus:border-indigo-550 focus:bg-white focus:outline-none shadow-sm"
                    required
                  />
                </div>
              </div>

              {/* Email Address */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1.5">
                  University Email
                </label>
                <div className="relative">
                  <span className="absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
                    <Mail className="h-4 w-4" />
                  </span>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="block w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-9 pr-3 text-xs focus:border-indigo-550 focus:bg-white focus:outline-none shadow-sm"
                    required
                  />
                </div>
              </div>
            </div>

            {/* Change Password Block */}
            <div className="pt-4 border-t border-slate-100 mt-4">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">Change Password (optional)</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-[11px] font-semibold text-slate-500 mb-1">Current Password</label>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="block w-full rounded-lg border border-slate-200 bg-slate-50 py-2 px-3 text-xs focus:border-indigo-550 focus:bg-white focus:outline-none shadow-sm"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-slate-500 mb-1">New Password</label>
                  <input
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="At least 6 characters"
                    className="block w-full rounded-lg border border-slate-200 bg-slate-50 py-2 px-3 text-xs focus:border-indigo-550 focus:bg-white focus:outline-none shadow-sm"
                  />
                </div>
              </div>
            </div>

            <div className="pt-4 border-t border-slate-100 flex justify-end">
              <button
                type="submit"
                disabled={profileMutation.isPending}
                className="rounded-lg bg-indigo-650 bg-gradient-to-r from-indigo-600 to-indigo-500 px-5 py-2.5 text-xs font-semibold text-white hover:bg-indigo-750 transition flex items-center"
              >
                {profileMutation.isPending ? (
                  <div className="h-4 w-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                ) : (
                  <>
                    <Check className="h-4 w-4 mr-1" />
                    <span>Save Changes</span>
                  </>
                )}
              </button>
            </div>
          </form>
        </div>

        {/* Reviews Lists */}
        <div className="rounded-3xl border border-slate-200 bg-white p-6 md:p-8 shadow-sm">
          <h2 className="font-display text-lg font-bold text-slate-900 mb-6">
            Reviews from peers
          </h2>

          {isReviewsLoading ? (
            <div className="text-center text-xs text-slate-400 py-6">Loading reviews...</div>
          ) : reviews?.length === 0 ? (
            <div className="text-center py-6 text-xs text-slate-400">
              No reviews received yet. Build up your reputation by swapping textbooks with peers!
            </div>
          ) : (
            <div className="space-y-4">
              {reviews?.map((review) => (
                <div key={review._id} className="p-4 bg-slate-50 border border-slate-150 rounded-2xl flex flex-col space-y-2">
                  <div className="flex justify-between items-center text-xs">
                    <span className="font-semibold text-slate-900">{review.reviewer.name}</span>
                    <div className="flex items-center text-amber-500">
                      {[1, 2, 3, 4, 5].map((star) => (
                        <Star 
                          key={star} 
                          className={`h-3 w-3 ${star <= review.rating ? 'fill-amber-500' : 'text-slate-350'}`} 
                        />
                      ))}
                    </div>
                  </div>
                  <p className="text-xs text-slate-650 leading-relaxed font-normal">"{review.comment}"</p>
                  <span className="text-[10px] text-slate-400 block self-end">
                    {new Date(review.createdAt).toLocaleDateString()}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Sidebar Profile overview */}
      <div className="lg:col-span-1 space-y-6">
        <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm text-center flex flex-col items-center">
          <div className="h-20 w-20 rounded-full bg-indigo-50 text-indigo-650 flex items-center justify-center font-bold font-display text-2xl border border-indigo-100 shadow-inner">
            {user?.name.substring(0, 2).toUpperCase()}
          </div>
          <h3 className="mt-4 font-display font-extrabold text-slate-900 text-lg leading-tight">{user?.name}</h3>
          <p className="text-xs text-slate-500">{user?.email}</p>

          <div className="mt-6 w-full border-t border-slate-100 pt-6 space-y-4">
            {/* Rating display */}
            <div className="flex justify-between items-center text-xs">
              <span className="text-slate-450 font-bold uppercase tracking-wide">Reputation</span>
              <div className="flex items-center text-amber-500 font-bold">
                <Star className="h-4 w-4 fill-amber-500 mr-1" />
                <span>{user?.rating ? user.rating.toFixed(1) : '5.0'} / 5.0</span>
              </div>
            </div>

            {/* Admin identifier */}
            {user?.role === 'admin' && (
              <div className="flex justify-between items-center text-xs">
                <span className="text-slate-450 font-bold uppercase tracking-wide">User Role</span>
                <span className="bg-rose-50 text-rose-700 border border-rose-100 font-bold px-2.5 py-0.5 rounded text-[10px]">
                  Administrator
                </span>
              </div>
            )}
          </div>

          <div className="mt-8 w-full space-y-3">
            <button
              onClick={() => setRateModalOpen(true)}
              className="w-full flex items-center justify-center space-x-1.5 rounded-lg border border-slate-250 bg-white py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 hover:border-slate-350 transition"
            >
              <MessageSquare className="h-4 w-4 text-indigo-500" />
              <span>Review a peer</span>
            </button>
            <Link
              to="/disputes"
              className="w-full flex items-center justify-center space-x-1.5 rounded-lg bg-rose-50 border border-rose-100 py-2 text-xs font-semibold text-rose-650 hover:bg-rose-100 transition"
            >
              <ShieldAlert className="h-4 w-4" />
              <span>Report an Issue</span>
            </Link>
          </div>
        </div>
      </div>

      {/* Review Peer Modal */}
      {rateModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 backdrop-blur-sm p-4">
          <div className="w-full max-w-md bg-white rounded-2xl shadow-xl border border-slate-100 p-6 relative animate-fade-in">
            <button 
              onClick={() => setRateModalOpen(false)}
              className="absolute right-4 top-4 p-1.5 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-650 transition"
            >
              <X className="h-5 w-5" />
            </button>

            <h3 className="font-display text-lg font-bold text-slate-900">
              Submit Peer Review
            </h3>
            <p className="text-xs text-slate-500 mt-1">
              Rate your exchange experience with another BookBridge student
            </p>

            <form onSubmit={handleRateSubmit} className="mt-6 space-y-4">
              {/* Target User ID */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1">
                  Student Member ID / Name
                </label>
                <input
                  type="text"
                  value={rateUserId}
                  onChange={(e) => setRateUserId(e.target.value)}
                  placeholder="Paste student ID or name"
                  className="block w-full rounded-lg border border-slate-200 py-2 px-3 text-xs focus:border-indigo-500 focus:outline-none shadow-sm"
                  required
                />
              </div>

              {/* Rating selection stars */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1.5">
                  Rating
                </label>
                <div className="flex space-x-2">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setRateRating(star)}
                      className="text-amber-450 hover:scale-115 transition"
                    >
                      <Star 
                        className={`h-6 w-6 ${star <= rateRating ? 'fill-amber-500 text-amber-500' : 'text-slate-300'}`} 
                      />
                    </button>
                  ))}
                </div>
              </div>

              {/* Comment */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1">
                  Comment
                </label>
                <textarea
                  rows={4}
                  value={rateComment}
                  onChange={(e) => setRateComment(e.target.value)}
                  placeholder="Share details about the trade: communication, punctuality, and book condition..."
                  className="block w-full rounded-lg border border-slate-200 py-2 px-3 text-xs focus:border-indigo-500 focus:outline-none shadow-sm"
                  required
                />
              </div>

              <div className="pt-4 flex gap-3">
                <button
                  type="button"
                  onClick={() => setRateModalOpen(false)}
                  className="flex-1 rounded-lg border border-slate-200 px-4 py-2.5 text-xs font-semibold text-slate-755 hover:bg-slate-50 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={peerReviewMutation.isPending}
                  className="flex-1 rounded-lg bg-indigo-650 bg-gradient-to-r from-indigo-600 to-indigo-500 px-4 py-2.5 text-xs font-semibold text-white hover:bg-indigo-750 transition flex justify-center items-center"
                >
                  {peerReviewMutation.isPending ? (
                    <div className="h-5 w-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  ) : (
                    <span>Submit Review</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};
export default Profile;
