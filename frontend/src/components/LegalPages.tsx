import { ArrowLeft, AlertCircle } from 'lucide-react';

interface LegalPageProps {
  onBackToStore: () => void;
}

export const TermsPage: React.FC<LegalPageProps> = ({ onBackToStore }) => {
  return (
    <div className="min-h-screen bg-[#f2f8f3] text-gray-900 py-10 px-4 sm:px-6 lg:px-8 font-sans">
      <div className="max-w-4xl mx-auto space-y-8">
        <button
          onClick={onBackToStore}
          className="inline-flex items-center gap-2 text-xs font-semibold text-emerald-800 hover:text-emerald-950 transition"
        >
          <ArrowLeft className="h-4 w-4" /> Back to Store
        </button>

        <div className="bg-white rounded-3xl p-6 sm:p-10 border border-emerald-200 shadow-sm space-y-6">
          <div className="border-b border-gray-100 pb-5">
            <span className="px-3 py-1 rounded-full bg-emerald-100 text-emerald-900 text-[11px] font-bold tracking-wide border border-emerald-200 inline-block mb-2">
              LEGAL TERMS
            </span>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-emerald-950 font-serif">
              Terms & Conditions
            </h1>
            <p className="text-xs text-gray-500 mt-1">
              Effective Date: [LAUNCH_DATE_PLACEHOLDER] | Last Updated: Pre-Production Draft
            </p>
          </div>

          <div className="p-3.5 bg-amber-50 border border-amber-200 rounded-xl flex items-start gap-3 text-xs text-amber-900">
            <AlertCircle className="h-5 w-5 text-amber-700 shrink-0 mt-0.5" />
            <div>
              <strong className="block font-bold">Notice to Business Owner:</strong>
              This policy contains standard pre-launch legal terms. Placeholders marked in brackets must be reviewed and customized before live production deployment as documented in <code className="font-mono text-[11px]">docs/BUSINESS_INFORMATION_REQUIRED_BEFORE_LAUNCH.md</code>.
            </div>
          </div>

          <div className="space-y-6 text-xs sm:text-sm text-gray-700 leading-relaxed">
            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">1. Introduction & Acceptance of Terms</h2>
              <p>
                Welcome to <strong>RR Jaggery Traders</strong> (referred to herein as "we", "us", or "our"), operating from [REGISTERED_OFFICE_ADDRESS_PLACEHOLDER - Vollurupallem, Thotlavalluru, Andhra Pradesh 5221163]. By accessing our website, placing an order, or registering for an account, you ("Customer", "User", or "You") agree to be bound by these Terms and Conditions.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">2. Business & Legal Entity Information</h2>
              <p>
                <strong>Legal Entity Name:</strong> [LEGAL_ENTITY_NAME_PLACEHOLDER]<br />
                <strong>GSTIN:</strong> [GSTIN_REGISTRATION_NUMBER_PLACEHOLDER]<br />
                <strong>FSSAI Registration / License Number:</strong> [FSSAI_LICENSE_NUMBER_PLACEHOLDER]<br />
                <strong>Official Email:</strong> [SUPPORT_EMAIL_PLACEHOLDER]<br />
                <strong>Official Contact:</strong> [SUPPORT_PHONE_PLACEHOLDER]
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">3. Products & Pricing</h2>
              <p>
                We specialize in naturally processed sugarcane jaggery products, including jaggery powder, blocks, and syrup.
              </p>
              <ul className="list-disc pl-5 space-y-1 text-xs">
                <li>All prices listed are subject to applicable Goods and Services Tax (GST) as specified at checkout.</li>
                <li>Product weights and specifications are measured at the time of packing. Minor natural variations in color, moisture, or texture may occur due to traditional artisan boiling processes.</li>
                <li>Wholesale tier pricing and minimum order quantities (MOQ) apply strictly to wholesale orders as configured by our management.</li>
              </ul>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">4. User Accounts & Security</h2>
              <p>
                Retail customers may register accounts for faster checkout and order tracking. You are responsible for maintaining the confidentiality of your login credentials. We reserve the right to suspend or terminate accounts that violate security protocols or provide inaccurate information.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">5. Governing Law & Dispute Resolution</h2>
              <p>
                These Terms and Conditions shall be governed by and construed in accordance with the laws of India. Any disputes arising in connection with these terms shall be subject to the exclusive jurisdiction of the competent courts in [LEGAL_JURISDICTION_CITY_PLACEHOLDER - e.g. Krishna District / Vijayawada, Andhra Pradesh].
              </p>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
};

export const PrivacyPage: React.FC<LegalPageProps> = ({ onBackToStore }) => {
  return (
    <div className="min-h-screen bg-[#f2f8f3] text-gray-900 py-10 px-4 sm:px-6 lg:px-8 font-sans">
      <div className="max-w-4xl mx-auto space-y-8">
        <button
          onClick={onBackToStore}
          className="inline-flex items-center gap-2 text-xs font-semibold text-emerald-800 hover:text-emerald-950 transition"
        >
          <ArrowLeft className="h-4 w-4" /> Back to Store
        </button>

        <div className="bg-white rounded-3xl p-6 sm:p-10 border border-emerald-200 shadow-sm space-y-6">
          <div className="border-b border-gray-100 pb-5">
            <span className="px-3 py-1 rounded-full bg-emerald-100 text-emerald-900 text-[11px] font-bold tracking-wide border border-emerald-200 inline-block mb-2">
              DATA PROTECTION
            </span>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-emerald-950 font-serif">
              Privacy Policy
            </h1>
            <p className="text-xs text-gray-500 mt-1">
              Effective Date: [LAUNCH_DATE_PLACEHOLDER] | RR Jaggery Traders
            </p>
          </div>

          <div className="p-3.5 bg-amber-50 border border-amber-200 rounded-xl flex items-start gap-3 text-xs text-amber-900">
            <AlertCircle className="h-5 w-5 text-amber-700 shrink-0 mt-0.5" />
            <div>
              <strong className="block font-bold">Notice to Business Owner:</strong>
              This Privacy Policy reflects data collection strictly necessary for fulfilling retail orders and authentication. Review and append grievance officer details as documented in <code className="font-mono text-[11px]">docs/BUSINESS_INFORMATION_REQUIRED_BEFORE_LAUNCH.md</code>.
            </div>
          </div>

          <div className="space-y-6 text-xs sm:text-sm text-gray-700 leading-relaxed">
            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">1. Information We Collect</h2>
              <p>
                We collect personal information necessary to deliver our products and maintain secure account access:
              </p>
              <ul className="list-disc pl-5 space-y-1 text-xs">
                <li><strong>Identity & Contact Details:</strong> Full name, email address, and phone number provided upon registration or checkout.</li>
                <li><strong>Delivery Address:</strong> Recipient name, street address, city, state, and postal code for shipping execution.</li>
                <li><strong>Order History & Invoices:</strong> Records of past purchases, product quantities, GST tax invoices, and payment statuses.</li>
                <li><strong>Authentication Data:</strong> Secure salted-hash password storage and session JWT tokens. We never store raw passwords.</li>
              </ul>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">2. How We Use Your Information</h2>
              <ul className="list-disc pl-5 space-y-1 text-xs">
                <li>To process, confirm, dispatch, and deliver your retail jaggery orders.</li>
                <li>To generate statutory GST tax invoices and accounting records.</li>
                <li>To provide customer support and notify you of order status changes.</li>
                <li>To safeguard against fraudulent transactions and ensure system integrity.</li>
              </ul>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">3. Information Sharing & Third Parties</h2>
              <p>
                We do not sell, rent, or trade your personal information. Data is shared exclusively with:
              </p>
              <ul className="list-disc pl-5 space-y-1 text-xs">
                <li>Logistics and delivery partners strictly for package delivery.</li>
                <li>Authorized payment gateway processors for processing payments (we do not store credit card or UPI credentials).</li>
                <li>Statutory authorities when strictly mandated under Indian law or GST compliance audits.</li>
              </ul>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">4. Data Security</h2>
              <p>
                All data transmission between your browser and our servers is encrypted using industry-standard TLS/HTTPS protocols. Passwords are cryptographically hashed using BCrypt. Access to customer records is restricted to authorized operations personnel.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">5. Grievance Redressal Officer</h2>
              <p>
                In accordance with Information Technology Rules, the details of our Grievance Officer are:<br />
                <strong>Name:</strong> [GRIEVANCE_OFFICER_NAME_PLACEHOLDER]<br />
                <strong>Designation:</strong> [GRIEVANCE_OFFICER_DESIGNATION_PLACEHOLDER]<br />
                <strong>Email:</strong> [GRIEVANCE_OFFICER_EMAIL_PLACEHOLDER]<br />
                <strong>Address:</strong> Vollurupallem, Thotlavalluru, Andhra Pradesh 5221163
              </p>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
};

export const RefundPolicyPage: React.FC<LegalPageProps> = ({ onBackToStore }) => {
  return (
    <div className="min-h-screen bg-[#f2f8f3] text-gray-900 py-10 px-4 sm:px-6 lg:px-8 font-sans">
      <div className="max-w-4xl mx-auto space-y-8">
        <button
          onClick={onBackToStore}
          className="inline-flex items-center gap-2 text-xs font-semibold text-emerald-800 hover:text-emerald-950 transition"
        >
          <ArrowLeft className="h-4 w-4" /> Back to Store
        </button>

        <div className="bg-white rounded-3xl p-6 sm:p-10 border border-emerald-200 shadow-sm space-y-6">
          <div className="border-b border-gray-100 pb-5">
            <span className="px-3 py-1 rounded-full bg-emerald-100 text-emerald-900 text-[11px] font-bold tracking-wide border border-emerald-200 inline-block mb-2">
              RETURNS & REFUNDS
            </span>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-emerald-950 font-serif">
              Refund & Cancellation Policy
            </h1>
            <p className="text-xs text-gray-500 mt-1">
              RR Jaggery Traders — Food Quality & Customer Satisfaction
            </p>
          </div>

          <div className="p-3.5 bg-amber-50 border border-amber-200 rounded-xl flex items-start gap-3 text-xs text-amber-900">
            <AlertCircle className="h-5 w-5 text-amber-700 shrink-0 mt-0.5" />
            <div>
              <strong className="block font-bold">Notice to Business Owner:</strong>
              Because jaggery is a consumable agricultural food product, refund conditions, timelines, and transit inspection windows must be approved by the owner as outlined in <code className="font-mono text-[11px]">docs/BUSINESS_INFORMATION_REQUIRED_BEFORE_LAUNCH.md</code>.
            </div>
          </div>

          <div className="space-y-6 text-xs sm:text-sm text-gray-700 leading-relaxed">
            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">1. Order Cancellations</h2>
              <p>
                Customers may cancel an order directly from their account dashboard while the order status remains in <strong>PENDING</strong> or <strong>CONFIRMED</strong> prior to dispatch. Once an order has reached <strong>DISPATCHED</strong> status with our courier partner, it cannot be cancelled.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">2. Consumable Food Product Policy</h2>
              <p>
                Due to food safety and hygiene regulations, opened or consumed jaggery packages cannot be accepted for return. We accept return requests only under the following verified circumstances:
              </p>
              <ul className="list-disc pl-5 space-y-1 text-xs">
                <li>Package delivered was visibly damaged or tampered with during transit.</li>
                <li>Incorrect product variant or SKU delivered (e.g. powder instead of blocks).</li>
                <li>Verified quality defect or contamination reported within [RETURN_NOTIFICATION_HOURS_PLACEHOLDER - e.g. 48 hours] of delivery.</li>
              </ul>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">3. Refund Processing Timeline</h2>
              <p>
                Upon receipt and verification of an eligible cancellation or transit defect claim:
              </p>
              <ul className="list-disc pl-5 space-y-1 text-xs">
                <li>Approved refunds are initiated to the original payment source within [REFUND_PROCESSING_DAYS_PLACEHOLDER - e.g. 5-7 business days].</li>
                <li>For Cash on Delivery orders, refunds will be processed via direct bank transfer (NEFT/IMPS) upon verification of account details.</li>
              </ul>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">4. How to Request Support</h2>
              <p>
                To raise a cancellation or return query, please contact our team with your Order ID and photo proof (if damaged in transit):<br />
                <strong>Email:</strong> [SUPPORT_EMAIL_PLACEHOLDER]<br />
                <strong>Helpline:</strong> [SUPPORT_PHONE_PLACEHOLDER]
              </p>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
};

export const ShippingPolicyPage: React.FC<LegalPageProps> = ({ onBackToStore }) => {
  return (
    <div className="min-h-screen bg-[#f2f8f3] text-gray-900 py-10 px-4 sm:px-6 lg:px-8 font-sans">
      <div className="max-w-4xl mx-auto space-y-8">
        <button
          onClick={onBackToStore}
          className="inline-flex items-center gap-2 text-xs font-semibold text-emerald-800 hover:text-emerald-950 transition"
        >
          <ArrowLeft className="h-4 w-4" /> Back to Store
        </button>

        <div className="bg-white rounded-3xl p-6 sm:p-10 border border-emerald-200 shadow-sm space-y-6">
          <div className="border-b border-gray-100 pb-5">
            <span className="px-3 py-1 rounded-full bg-emerald-100 text-emerald-900 text-[11px] font-bold tracking-wide border border-emerald-200 inline-block mb-2">
              LOGISTICS & DELIVERY
            </span>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-emerald-950 font-serif">
              Shipping & Delivery Policy
            </h1>
            <p className="text-xs text-gray-500 mt-1">
              Direct from our factory works at Vollurupallem, Thotlavalluru, Andhra Pradesh
            </p>
          </div>

          <div className="p-3.5 bg-amber-50 border border-amber-200 rounded-xl flex items-start gap-3 text-xs text-amber-900">
            <AlertCircle className="h-5 w-5 text-amber-700 shrink-0 mt-0.5" />
            <div>
              <strong className="block font-bold">Notice to Business Owner:</strong>
              Courier partners, shipping fee thresholds, and average transit delivery schedules must be verified by the owner prior to live launch as detailed in <code className="font-mono text-[11px]">docs/BUSINESS_INFORMATION_REQUIRED_BEFORE_LAUNCH.md</code>.
            </div>
          </div>

          <div className="space-y-6 text-xs sm:text-sm text-gray-700 leading-relaxed">
            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">1. Shipping Coverage</h2>
              <p>
                We currently fulfill retail and wholesale jaggery deliveries across [SHIPPING_SERVICEABLE_REGIONS_PLACEHOLDER - Pan India / South India serviceable pin codes]. Serviceability is confirmed at checkout based on your destination postal pin code.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">2. Processing & Dispatch Timelines</h2>
              <p>
                Orders are processed and packaged at our works in Vollurupallem:
              </p>
              <ul className="list-disc pl-5 space-y-1 text-xs">
                <li>Standard retail orders are dispatched within [DISPATCH_TIMELINE_PLACEHOLDER - e.g. 24 to 48 business hours] of order confirmation.</li>
                <li>Estimated delivery transit time is typically [ESTIMATED_DELIVERY_DAYS_PLACEHOLDER - e.g. 3 to 7 business days], depending on destination location and courier service availability.</li>
                <li>Orders placed on Sundays or public holidays are processed on the subsequent working day.</li>
              </ul>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">3. Shipping Charges</h2>
              <p>
                Shipping charges are calculated transparently during checkout based on order subtotal:
              </p>
              <ul className="list-disc pl-5 space-y-1 text-xs">
                <li>Orders qualifying for free delivery are marked accordingly during checkout.</li>
                <li>Standard shipping charges apply for orders below the threshold as shown on the order summary prior to payment.</li>
              </ul>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">4. Tracking & Delivery Inspection</h2>
              <p>
                Upon dispatch, tracking information and dispatch confirmation are updated in your customer portal order history. Customers are requested to inspect package integrity before accepting delivery from the courier agent.
              </p>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
};
