"use client";

import { useState, useEffect, useRef } from "react";
import AppLayout from "@/components/Layout/AppLayout";
import PermissionGuard from "@/components/PermissionGuard";
import api from "@/lib/api";
import { useAuth } from "@/contexts/AuthContext";
import { usePathname } from "next/navigation";

export default function AiAssistantPage() {
  const { user, hasPermission } = useAuth();
  const pathname = usePathname();
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [context, setContext] = useState(null);
  const [suggestedActions, setSuggestedActions] = useState([]);
  const [conversations, setConversations] = useState([]);
  const [selectedConversation, setSelectedConversation] = useState(null);
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    loadContext();
    loadConversations();
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  const loadContext = async () => {
    try {
      const ctx = await api.getAiContext(pathname, "ai-assistant", {});
      setContext(ctx);
    } catch (err) {
      console.error("Error loading AI context:", err);
    }
  };

  const loadConversations = async () => {
    try {
      const data = await api.getAiConversations();
      setConversations(data || []);
    } catch (err) {
      console.error("Error loading conversations:", err);
    }
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!inputMessage.trim() || isLoading) return;

    const userMessage = {
      role: "user",
      content: inputMessage,
      timestamp: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputMessage("");
    setIsLoading(true);
    setError(null);

    try {
      const response = await api.sendAiMessage({
        message: inputMessage,
        conversationId: selectedConversation?.id || null,
        currentPage: pathname,
        currentModule: "ai-assistant",
      });

      const aiMessage = {
        role: "assistant",
        content: response.response || response.message || "I'm here to help!",
        timestamp: new Date().toISOString(),
      };

      if (response.suggestedActions && response.suggestedActions.length > 0) {
        setSuggestedActions(response.suggestedActions);
      }

      setMessages((prev) => [...prev, aiMessage]);

      if (response.conversationId && !selectedConversation) {
        loadConversations();
      }
    } catch (err) {
      setError(err.message || "Failed to send message");
      const errorMessage = {
        role: "assistant",
        content: `Error: ${err.message || "Failed to process your message"}`,
        timestamp: new Date().toISOString(),
        isError: true,
      };
      setMessages((prev) => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
      inputRef.current?.focus();
    }
  };

  const handleExecuteAction = async (action) => {
    if (!action.confirmationToken && action.requiresConfirmation) {
      alert("This action requires confirmation. Please confirm in the chat.");
      return;
    }

    try {
      setIsLoading(true);
      const response = await api.executeAiAction({
        actionId: action.id,
        confirmationToken: action.confirmationToken,
        parameters: action.parameters || {},
      });

      const resultMessage = {
        role: "assistant",
        content: response.message || `Action "${action.name}" executed successfully!`,
        timestamp: new Date().toISOString(),
      };

      setMessages((prev) => [...prev, resultMessage]);
      setSuggestedActions([]);
    } catch (err) {
      setError(err.message || "Failed to execute action");
      alert(err.message || "Failed to execute action");
    } finally {
      setIsLoading(false);
    }
  };

  const formatTimestamp = (timestamp) => {
    if (!timestamp) return "";
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  };

  return (
    <AppLayout title="AI Assistant" subtitle="Get help with operations">
      <PermissionGuard permission="USE_AI_AGENT">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 h-[calc(100vh-200px)]">
          {/* Conversations Sidebar */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 h-full flex flex-col">
              <h3 className="text-lg font-semibold text-slate-900 mb-4">Conversations</h3>
              <div className="flex-1 overflow-y-auto space-y-2 mb-4">
                <button
                  onClick={() => {
                    setSelectedConversation(null);
                    setMessages([]);
                    setSuggestedActions([]);
                  }}
                  className={`w-full text-left px-3 py-2 rounded-lg transition-colors ${
                    !selectedConversation
                      ? "bg-slate-900 text-white"
                      : "bg-slate-50 hover:bg-slate-100 text-slate-900"
                  }`}
                >
                  <div className="font-medium">New Conversation</div>
                </button>
                {conversations.map((conv) => (
                  <button
                    key={conv.id}
                    onClick={() => {
                      setSelectedConversation(conv);
                      // Load conversation messages
                      setMessages([]);
                    }}
                    className={`w-full text-left px-3 py-2 rounded-lg transition-colors ${
                      selectedConversation?.id === conv.id
                        ? "bg-slate-900 text-white"
                        : "bg-slate-50 hover:bg-slate-100 text-slate-900"
                    }`}
                  >
                    <div className="font-medium text-sm truncate">
                      {conv.title || `Conversation ${conv.id}`}
                    </div>
                    <div
                      className={`text-xs mt-1 ${
                        selectedConversation?.id === conv.id ? "text-slate-200" : "text-slate-500"
                      }`}
                    >
                      {conv.createdAt
                        ? new Date(conv.createdAt).toLocaleDateString()
                        : "Recent"}
                    </div>
                  </button>
                ))}
              </div>
              {context && (
                <div className="pt-4 border-t border-slate-200">
                  <div className="text-xs text-slate-500 mb-2">Context</div>
                  <div className="text-xs text-slate-700">
                    <div>User: {context.userName || user?.username || "N/A"}</div>
                    <div>Role: {context.userRole || "N/A"}</div>
                    <div>Page: {context.currentPage || pathname}</div>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Chat Area */}
          <div className="lg:col-span-3">
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 h-full flex flex-col">
              {/* Chat Header */}
              <div className="px-6 py-4 border-b border-slate-200">
                <h3 className="text-lg font-semibold text-slate-900">AI Assistant</h3>
                <p className="text-sm text-slate-500">Ask questions and get help with operations</p>
              </div>

              {/* Messages */}
              <div className="flex-1 overflow-y-auto p-6 space-y-4">
                {messages.length === 0 ? (
                  <div className="text-center py-12">
                    <div className="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-4">
                      <svg
                        className="w-8 h-8 text-slate-600"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"
                        />
                      </svg>
                    </div>
                    <p className="text-slate-600">Start a conversation with the AI assistant</p>
                    <p className="text-sm text-slate-500 mt-2">
                      Ask questions, request reports, or get help with operations
                    </p>
                  </div>
                ) : (
                  messages.map((message, idx) => (
                    <div
                      key={idx}
                      className={`flex ${message.role === "user" ? "justify-end" : "justify-start"}`}
                    >
                      <div
                        className={`max-w-[80%] rounded-lg px-4 py-2 ${
                          message.role === "user"
                            ? "bg-slate-900 text-white"
                            : message.isError
                            ? "bg-red-50 text-red-700 border border-red-200"
                            : "bg-slate-100 text-slate-900"
                        }`}
                      >
                        <div className="text-sm whitespace-pre-wrap">{message.content}</div>
                        <div
                          className={`text-xs mt-1 ${
                            message.role === "user" ? "text-slate-300" : "text-slate-500"
                          }`}
                        >
                          {formatTimestamp(message.timestamp)}
                        </div>
                      </div>
                    </div>
                  ))
                )}

                {isLoading && (
                  <div className="flex justify-start">
                    <div className="bg-slate-100 rounded-lg px-4 py-2">
                      <div className="flex items-center gap-2">
                        <div className="w-2 h-2 bg-slate-600 rounded-full animate-bounce"></div>
                        <div className="w-2 h-2 bg-slate-600 rounded-full animate-bounce" style={{ animationDelay: "0.2s" }}></div>
                        <div className="w-2 h-2 bg-slate-600 rounded-full animate-bounce" style={{ animationDelay: "0.4s" }}></div>
                      </div>
                    </div>
                  </div>
                )}

                {suggestedActions.length > 0 && (
                  <div className="mt-4 space-y-2">
                    <div className="text-sm font-medium text-slate-700 mb-2">Suggested Actions:</div>
                    {suggestedActions.map((action, idx) => (
                      <div
                        key={idx}
                        className="bg-blue-50 border border-blue-200 rounded-lg p-3"
                      >
                        <div className="font-medium text-sm text-blue-900 mb-1">{action.name}</div>
                        {action.description && (
                          <div className="text-xs text-blue-700 mb-2">{action.description}</div>
                        )}
                        <button
                          onClick={() => handleExecuteAction(action)}
                          disabled={isLoading}
                          className="text-xs px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors disabled:opacity-50"
                        >
                          {action.requiresConfirmation ? "Confirm & Execute" : "Execute"}
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                <div ref={messagesEndRef} />
              </div>

              {/* Error Message */}
              {error && (
                <div className="px-6 py-2 bg-red-50 border-t border-red-200">
                  <p className="text-sm text-red-700">{error}</p>
                </div>
              )}

              {/* Input */}
              <div className="px-6 py-4 border-t border-slate-200">
                <form onSubmit={handleSendMessage} className="flex gap-2">
                  <input
                    ref={inputRef}
                    type="text"
                    value={inputMessage}
                    onChange={(e) => setInputMessage(e.target.value)}
                    placeholder="Type your message..."
                    disabled={isLoading}
                    className="flex-1 px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none disabled:opacity-50"
                  />
                  <button
                    type="submit"
                    disabled={isLoading || !inputMessage.trim()}
                    className="px-6 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed font-medium"
                  >
                    Send
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
      </PermissionGuard>
    </AppLayout>
  );
}
