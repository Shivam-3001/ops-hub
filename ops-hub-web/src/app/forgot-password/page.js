"use client";

import { useState } from "react";
import Link from "next/link";
import api from "@/lib/api";

const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;

export default function ForgotPasswordPage() {
  const [step, setStep] = useState("lookup");
  const [username, setUsername] = useState("");
  const [channels, setChannels] = useState([]);
  const [selectedChannel, setSelectedChannel] = useState("");
  const [channelValue, setChannelValue] = useState("");
  const [otp, setOtp] = useState("");
  const [resetToken, setResetToken] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const handleLookup = async (event) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    setLoading(true);

    try {
      const response = await api.lookupForgotPassword(username.trim());
      setChannels(response.channels || []);
      setSelectedChannel("");
      setChannelValue("");
      setOtp("");
      setResetToken("");
      setNewPassword("");
      setConfirmPassword("");
      setStep("channel");
    } catch (err) {
      setError(err.message || "Unable to find user.");
    } finally {
      setLoading(false);
    }
  };

  const handleSendOtp = async () => {
    setError(null);
    setSuccess(null);
    setLoading(true);
    try {
      await api.sendForgotPasswordOtp({
        username: username.trim(),
        channel: selectedChannel,
        value: channelValue.trim(),
      });
      setStep("otp");
      setSuccess("OTP sent. It is valid for 3 minutes.");
    } catch (err) {
      setError(err.message || "Failed to send OTP.");
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (event) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    setLoading(true);

    try {
      const response = await api.verifyForgotPasswordOtp({
        username: username.trim(),
        channel: selectedChannel,
        otp: otp.trim(),
      });
      setResetToken(response.resetToken);
      setStep("reset");
      setSuccess("OTP verified. Please set a new password.");
    } catch (err) {
      setError(err.message || "OTP verification failed.");
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async (event) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);

    if (!passwordRegex.test(newPassword)) {
      setError(
        "Password must be at least 8 characters and include uppercase, lowercase, number, and special character."
      );
      return;
    }

    if (newPassword !== confirmPassword) {
      setError("New password and confirm password do not match.");
      return;
    }

    setLoading(true);
    try {
      await api.resetForgotPassword({
        username: username.trim(),
        resetToken,
        newPassword,
        confirmPassword,
      });
      setStep("done");
      setSuccess("Password updated successfully. You can log in now.");
    } catch (err) {
      setError(err.message || "Failed to update password.");
    } finally {
      setLoading(false);
    }
  };

  const selectedChannelLabel = channels.find((ch) => ch.type === selectedChannel)?.maskedValue;

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-50 to-white dark:from-slate-950 dark:to-slate-900 px-6">
      <div className="w-full max-w-lg bg-white dark:bg-slate-900 rounded-2xl shadow-xl border border-slate-200 dark:border-slate-800 p-8">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Forgot Password</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Verify your account and set a new password.
          </p>
        </div>

        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
            {error}
          </div>
        )}
        {success && (
          <div className="mb-4 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg text-sm">
            {success}
          </div>
        )}

        {step === "lookup" && (
          <form onSubmit={handleLookup} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                Username / Employee ID
              </label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                className="w-full px-4 py-3 border border-slate-300 dark:border-slate-700 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none dark:bg-slate-900 dark:text-slate-100"
                placeholder="Enter your username"
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full bg-slate-900 text-white py-3 rounded-lg font-semibold hover:bg-slate-800 transition-colors disabled:opacity-60"
            >
              {loading ? "Checking..." : "Go"}
            </button>
          </form>
        )}

        {step === "channel" && (
          <div className="space-y-4">
            <div className="text-sm text-slate-600 dark:text-slate-400">
              Select a channel to receive your OTP.
            </div>
            <div className="space-y-3">
              {channels.map((channel) => (
                <label
                  key={channel.type}
                  className="flex items-center gap-3 border border-slate-200 dark:border-slate-700 rounded-lg px-4 py-3 cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800"
                >
                  <input
                    type="radio"
                    name="channel"
                    value={channel.type}
                    checked={selectedChannel === channel.type}
                    onChange={(e) => setSelectedChannel(e.target.value)}
                    className="h-4 w-4 text-slate-900"
                  />
                  <div className="text-sm text-slate-700 dark:text-slate-300">
                    {channel.type === "EMAIL" ? "Email" : "Mobile Number"} ({channel.maskedValue})
                  </div>
                </label>
              ))}
            </div>

            {selectedChannel && (
              <div className="space-y-3">
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300">
                  Confirm {selectedChannel === "EMAIL" ? "Email" : "Mobile Number"}
                </label>
                <input
                  type={selectedChannel === "EMAIL" ? "email" : "tel"}
                  value={channelValue}
                  onChange={(e) => setChannelValue(e.target.value)}
                  className="w-full px-4 py-3 border border-slate-300 dark:border-slate-700 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none dark:bg-slate-900 dark:text-slate-100"
                  placeholder={
                    selectedChannel === "EMAIL"
                      ? "Enter your email"
                      : "Enter your mobile number"
                  }
                />
                <button
                  type="button"
                  disabled={loading || !channelValue.trim()}
                  onClick={handleSendOtp}
                  className="w-full bg-slate-900 text-white py-3 rounded-lg font-semibold hover:bg-slate-800 transition-colors disabled:opacity-60"
                >
                  {loading ? "Sending..." : "Send OTP"}
                </button>
              </div>
            )}
          </div>
        )}

        {step === "otp" && (
          <form onSubmit={handleVerifyOtp} className="space-y-4">
            <div className="text-sm text-slate-600 dark:text-slate-400">
              OTP sent to {selectedChannel === "EMAIL" ? "email" : "mobile"}{" "}
              {selectedChannelLabel ? `(${selectedChannelLabel})` : ""}.
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">Enter OTP</label>
              <input
                type="text"
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
                className="w-full px-4 py-3 border border-slate-300 dark:border-slate-700 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none dark:bg-slate-900 dark:text-slate-100"
                placeholder="6-digit OTP"
              />
            </div>
            <button
              type="submit"
              disabled={loading || !otp.trim()}
              className="w-full bg-slate-900 text-white py-3 rounded-lg font-semibold hover:bg-slate-800 transition-colors disabled:opacity-60"
            >
              {loading ? "Verifying..." : "Verify OTP"}
            </button>
            <button
              type="button"
              disabled={loading}
              onClick={handleSendOtp}
              className="w-full border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-200 py-3 rounded-lg font-semibold hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors disabled:opacity-60"
            >
              Resend OTP
            </button>
          </form>
        )}

        {step === "reset" && (
          <form onSubmit={handleResetPassword} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                New Password
              </label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="w-full px-4 py-3 border border-slate-300 dark:border-slate-700 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none dark:bg-slate-900 dark:text-slate-100"
              />
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-2">
                Minimum 8 characters, 1 lowercase, 1 uppercase, 1 number, 1 special character.
              </p>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                Confirm New Password
              </label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="w-full px-4 py-3 border border-slate-300 dark:border-slate-700 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none dark:bg-slate-900 dark:text-slate-100"
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full bg-slate-900 text-white py-3 rounded-lg font-semibold hover:bg-slate-800 transition-colors disabled:opacity-60"
            >
              {loading ? "Updating..." : "Update Password"}
            </button>
          </form>
        )}

        {step === "done" && (
          <div className="space-y-4 text-center">
            <div className="text-sm text-slate-600 dark:text-slate-400">
              Password updated successfully.
            </div>
            <Link
              href="/login"
              className="inline-flex items-center justify-center w-full bg-slate-900 text-white py-3 rounded-lg font-semibold hover:bg-slate-800 transition-colors"
            >
              Back to Login
            </Link>
          </div>
        )}

        <div className="mt-6 text-center">
          <Link href="/login" className="text-sm text-slate-600 dark:text-slate-400 hover:text-slate-900">
            Back to Login
          </Link>
        </div>
      </div>
    </div>
  );
}
