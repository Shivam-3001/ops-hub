"use client";

import AppLayout from "@/components/Layout/AppLayout";
import { useAuth } from "@/contexts/AuthContext";

export default function DocumentPage() {
  const { user } = useAuth();
  const isAdmin = user?.userType === "ADMIN" || user?.role === "ADMIN";

  if (!isAdmin) {
    return (
      <AppLayout title="Document" subtitle="Project information and FAQs">
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-900 mb-2">Access restricted</h3>
          <p className="text-sm text-slate-600">
            This section is available to administrators only.
          </p>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout title="Document" subtitle="Project overview, modules, and FAQs">
      <div className="grid gap-6">
        <section className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-900 mb-3">Project Summary</h2>
          <p className="text-sm text-slate-600 leading-relaxed">
            Ops Hub is a role-based operations platform for customer lifecycle management,
            allocations, field visits, payments, notifications, and auditability. The backend
            is built with Spring Boot and MS SQL, while the frontend uses Next.js and Tailwind.
          </p>
        </section>

        <section className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">Core Modules</h2>
          <div className="grid md:grid-cols-2 gap-4 text-sm text-slate-600">
            <div className="border border-slate-100 rounded-lg p-4">
              <div className="font-medium text-slate-900 mb-1">Customer Management</div>
              <p>Upload, validate, allocate, and track customers with status lifecycle support.</p>
            </div>
            <div className="border border-slate-100 rounded-lg p-4">
              <div className="font-medium text-slate-900 mb-1">Allocations & Visits</div>
              <p>Assign customers by geography and track field visit outcomes.</p>
            </div>
            <div className="border border-slate-100 rounded-lg p-4">
              <div className="font-medium text-slate-900 mb-1">Payments</div>
              <p>Record payments and monitor pending amounts with escalation logic.</p>
            </div>
            <div className="border border-slate-100 rounded-lg p-4">
              <div className="font-medium text-slate-900 mb-1">AI Assistant</div>
              <p>Read-only, RBAC-aware summaries using local Ollama (Llama3).</p>
            </div>
            <div className="border border-slate-100 rounded-lg p-4">
              <div className="font-medium text-slate-900 mb-1">Notifications</div>
              <p>In-app alerts for allocations, payments, and escalations.</p>
            </div>
            <div className="border border-slate-100 rounded-lg p-4">
              <div className="font-medium text-slate-900 mb-1">Audit Trail</div>
              <p>Tracks who changed what and when, with filterable logs.</p>
            </div>
          </div>
        </section>

        <section className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-900 mb-3">Data & Security</h2>
          <ul className="text-sm text-slate-600 space-y-2">
            <li>RBAC enforced using roles and permissions mapped to geography.</li>
            <li>Customer data uses encrypted email/phone fields at rest.</li>
            <li>All uploads are validated for duplicates and required fields.</li>
            <li>Audit logs capture sensitive actions such as password changes and uploads.</li>
          </ul>
        </section>

        <section className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">FAQs</h2>
          <div className="space-y-4 text-sm text-slate-600">
            <div>
              <div className="font-medium text-slate-900">How is access controlled?</div>
              <p>
                Access is role-based and scoped by geography. Users only see data
                for their assigned areas and permissions.
              </p>
            </div>
            <div>
              <div className="font-medium text-slate-900">Is AI allowed to update data?</div>
              <p>No. The assistant is read-only and only summarizes authorized data.</p>
            </div>
            <div>
              <div className="font-medium text-slate-900">Why do uploads reject some rows?</div>
              <p>
                Duplicate phone/email, missing required fields, or invalid geography
                will mark rows as invalid.
              </p>
            </div>
            <div>
              <div className="font-medium text-slate-900">How do escalations work?</div>
              <p>
                Payments pending over 7 days escalate to Area Head; over 15 days to Circle Head.
              </p>
            </div>
          </div>
        </section>
      </div>
    </AppLayout>
  );
}
