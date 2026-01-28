"use client";

import { useState } from "react";
import { usePathname } from "next/navigation";
import PermissionGuard from "@/components/PermissionGuard";
import api from "@/lib/api";

export default function AiFloatingWidget() {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState("");
  const [conversationId, setConversationId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSend = async (e) => {
    e.preventDefault();
    if (!inputMessage.trim() || loading) return;

    const userMessage = {
      role: "user",
      content: inputMessage,
      timestamp: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputMessage("");
    setLoading(true);
    setError(null);

    try {
      const response = await api.askAi({
        question: userMessage.content,
        conversationId,
        currentPage: pathname,
        currentModule: "ai-widget",
        context: {},
      });

      if (response.conversationId && !conversationId) {
        setConversationId(response.conversationId);
      }

      const aiMessage = {
        role: "assistant",
        content: response.response || "I'm here to help.",
        timestamp: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, aiMessage]);
    } catch (err) {
      setError(err.message || "Failed to get AI response");
    } finally {
      setLoading(false);
    }
  };

  return (
    <PermissionGuard permission="USE_AI_AGENT">
      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        className="fixed bottom-6 right-6 z-50 rounded-full bg-slate-900 text-white w-12 h-12 shadow-lg hover:bg-slate-800"
        aria-label="AI Assistant"
      >
        🤖
      </button>

      {open && (
        <div className="fixed bottom-20 right-6 z-50 w-80 h-96 bg-white border border-slate-200 rounded-xl shadow-xl flex flex-col">
          <div className="px-4 py-3 border-b border-slate-200 flex items-center justify-between">
            <div className="text-sm font-semibold text-slate-900">AI Assistant</div>
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="text-slate-500 hover:text-slate-700"
              aria-label="Close AI Assistant"
            >
              ✕
            </button>
          </div>

          <div className="flex-1 overflow-y-auto p-3 space-y-3">
            {messages.length === 0 && (
              <div className="text-xs text-slate-500">
                Ask about customers, allocations, payments, visits, uploads, or notifications.
              </div>
            )}
            {messages.map((message, idx) => (
              <div
                key={`${message.role}-${idx}`}
                className={`text-xs p-2 rounded-lg ${
                  message.role === "user"
                    ? "bg-slate-900 text-white self-end"
                    : "bg-slate-100 text-slate-900"
                }`}
              >
                {message.content}
              </div>
            ))}
            {loading && <div className="text-xs text-slate-500">Thinking...</div>}
            {error && <div className="text-xs text-red-600">{error}</div>}
          </div>

          <form onSubmit={handleSend} className="border-t border-slate-200 p-3">
            <div className="flex gap-2">
              <input
                type="text"
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                className="flex-1 text-xs border border-slate-200 rounded-lg px-2 py-1 focus:outline-none focus:ring-2 focus:ring-slate-400"
                placeholder="Ask a question..."
              />
              <button
                type="submit"
                disabled={loading}
                className="text-xs bg-slate-900 text-white px-3 py-1 rounded-lg hover:bg-slate-800 disabled:opacity-60"
              >
                Send
              </button>
            </div>
          </form>
        </div>
      )}
    </PermissionGuard>
  );
}
