import { ArrowLeft } from 'lucide-react';

interface LegalPageProps {
  onBackToStore: () => void;
}

// ============================================================
// BUSINESS CONSTANTS — edit these only if business info changes
// ============================================================
const BIZ_NAME = 'RR Jaggery';
const BIZ_TYPE = 'Jaggery Manufacturing';
const BIZ_ADDRESS = 'Vollurupallem, Thotlavalluru, Andhra Pradesh – 521163';
const BIZ_EMAIL = 'rrjaggery@gmail.com';
const BIZ_PHONE = '+91 9948533315';
const BIZ_HOURS = '10:00 AM – 6:00 PM (Mon–Sat)';
const BIZ_GRIEVANCE_OFFICER = 'CH Padma';
// Do not infer GST registration status or add an unverified GSTIN.
// FSSAI: No FSSAI number available — do not invent one.

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
              Terms &amp; Conditions
            </h1>
            <p className="text-xs text-gray-500 mt-1">
              {BIZ_NAME} — {BIZ_TYPE}
            </p>
          </div>

          <div className="space-y-6 text-xs sm:text-sm text-gray-700 leading-relaxed">
            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">1. Introduction &amp; Acceptance of Terms</h2>
              <p>
                Welcome to <strong>{BIZ_NAME}</strong> ({BIZ_TYPE}), operating from {BIZ_ADDRESS}. By accessing our website,
                placing an order, or registering for an account, you ("Customer", "User", or "You") agree to be bound by
                these Terms and Conditions.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">2. Business Information</h2>
              <p>
                <strong>Business Name:</strong> {BIZ_NAME}<br />
                <strong>Business Type:</strong> {BIZ_TYPE}<br />
                <strong>Address:</strong> {BIZ_ADDRESS}<br />
                <strong>Email:</strong> {BIZ_EMAIL}<br />
                <strong>Phone:</strong> {BIZ_PHONE}<br />
                <strong>Support Hours:</strong> {BIZ_HOURS}
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">3. Products &amp; Pricing</h2>
              <p>
                We specialize in naturally processed sugarcane jaggery products, including jaggery powder, blocks, and syrup.
              </p>
              <ul className="list-disc pl-5 space-y-1 text-xs">
                <li>All prices listed are final. GST is not charged or included in the commercial order totals shown on this website.</li>
                <li>Product weights and specifications are measured at the time of packing. Minor natural variations in color, moisture, or texture may occur due to authentic, chemical-free processing methods.</li>
                <li>Wholesale tier pricing and minimum order quantities (MOQ) apply strictly to wholesale orders as configured by our management.</li>
              </ul>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">4. User Accounts &amp; Security</h2>
              <p>
                Retail customers may register accounts for faster checkout and order tracking. You are responsible for maintaining
                the confidentiality of your login credentials. We reserve the right to suspend or terminate accounts that violate
                security protocols or provide inaccurate information.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">5. Governing Law &amp; Dispute Resolution</h2>
              <p>
                These Terms and Conditions shall be governed by and construed in accordance with the laws of India. Any disputes
                arising in connection with these terms shall be subject to the exclusive jurisdiction of the competent courts in
                Krishna District / Vijayawada, Andhra Pradesh.
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
              {BIZ_NAME} — {BIZ_TYPE}
            </p>
          </div>

          <div className="space-y-6 text-xs sm:text-sm text-gray-700 leading-relaxed">
            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">1. Information We Collect</h2>
              <p>
                We collect personal information necessary to deliver our products and maintain secure account access:
              </p>
              <ul className="list-disc pl-5 space-y-1 text-xs">
                <li><strong>Identity &amp; Contact Details:</strong> Full name, email address, and phone number provided upon registration or checkout.</li>
                <li><strong>Delivery Address:</strong> Recipient name, street address, city, state, and postal code for shipping execution.</li>
                <li><strong>Order History &amp; Invoices:</strong> Records of past purchases, product quantities, payment amounts, and order statuses.</li>
                <li><strong>Authentication Data:</strong> Secure salted-hash password storage and session JWT tokens. We never store raw passwords.</li>
              </ul>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">2. How We Use Your Information</h2>
              <ul className="list-disc pl-5 space-y-1 text-xs">
                <li>To process, confirm, dispatch, and deliver your retail jaggery orders.</li>
                <li>To generate invoices and accounting records.</li>
                <li>To provide customer support and notify you of order status changes.</li>
                <li>To safeguard against fraudulent transactions and ensure system integrity.</li>
              </ul>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">3. Information Sharing &amp; Third Parties</h2>
              <p>
                We do not sell, rent, or trade your personal information. Data is shared exclusively with:
              </p>
              <ul className="list-disc pl-5 space-y-1 text-xs">
                <li>Logistics and delivery partners strictly for package delivery.</li>
                <li>Authorized payment gateway processors for processing payments (we do not store card or UPI credentials).</li>
                <li>Statutory authorities when strictly mandated under Indian law.</li>
              </ul>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">4. Data Security</h2>
              <p>
                All data transmission between your browser and our servers is encrypted using industry-standard TLS/HTTPS protocols.
                Passwords are cryptographically hashed using BCrypt. Access to customer records is restricted to authorized operations personnel.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">5. Grievance Redressal Officer</h2>
              <p>
                In accordance with Information Technology Rules, the details of our Grievance Officer are:<br />
                <strong>Name:</strong> {BIZ_GRIEVANCE_OFFICER}<br />
                <strong>Email:</strong> {BIZ_EMAIL}<br />
                <strong>Address:</strong> {BIZ_ADDRESS}<br />
                <strong>Contact Hours:</strong> {BIZ_HOURS}
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
              RETURNS &amp; REFUNDS
            </span>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-emerald-950 font-serif">
              Refund &amp; Cancellation Policy
            </h1>
            <p className="text-xs text-gray-500 mt-1">
              {BIZ_NAME} — {BIZ_TYPE}
            </p>
          </div>

          <div className="space-y-6 text-xs sm:text-sm text-gray-700 leading-relaxed">
            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">1. No Refund Policy</h2>
              <p>
                <strong>{BIZ_NAME} does not offer refunds.</strong> All sales are final. Once an order is placed and confirmed,
                it cannot be refunded under any circumstances.
              </p>
              <p>
                We encourage customers to review product descriptions, weights, and quantities carefully before placing an order.
                If you have questions prior to purchase, please contact us at <strong>{BIZ_EMAIL}</strong> or <strong>{BIZ_PHONE}</strong>.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">2. Order Cancellations</h2>
              <p>
                Customers may cancel an order directly from their account dashboard <strong>only while the order status remains PENDING</strong>,
                prior to confirmation and dispatch. Once an order has been confirmed or dispatched, it cannot be cancelled.
              </p>
              <p>
                <strong>Cancellation does not entitle the customer to a monetary refund.</strong> Any cancellation credit,
                if applicable, will be at the sole discretion of {BIZ_NAME} management.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">3. Consumable Food Product Notice</h2>
              <p>
                Jaggery is a consumable agricultural food product. Due to food safety and hygiene regulations, opened or
                consumed packages cannot be accepted for return under any circumstances.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">4. Contact</h2>
              <p>
                For order-related queries, please contact our team with your Order ID:<br />
                <strong>Email:</strong> {BIZ_EMAIL}<br />
                <strong>Phone:</strong> {BIZ_PHONE}<br />
                <strong>Hours:</strong> {BIZ_HOURS}
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
              LOGISTICS &amp; DELIVERY
            </span>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-emerald-950 font-serif">
              Shipping &amp; Delivery Policy
            </h1>
            <p className="text-xs text-gray-500 mt-1">
              Direct from our works at {BIZ_ADDRESS}
            </p>
          </div>

          <div className="space-y-6 text-xs sm:text-sm text-gray-700 leading-relaxed">
            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">1. Shipping Coverage</h2>
              <p>
                Fast delivery is available in and around <strong>Vijayawada and Guntur</strong>. For shipping to other locations,
                please contact our office for available shipping options.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">2. Delivery Timeline</h2>
              <p>
                Standard delivery timeline is <strong>3–5 business days</strong> from order confirmation, depending on location
                and courier availability.
              </p>
              <ul className="list-disc pl-5 space-y-1 text-xs">
                <li>Orders placed on Sundays or public holidays are processed on the subsequent working day.</li>
                <li>Delivery to locations outside the primary Vijayawada/Guntur region may take additional time.</li>
              </ul>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">3. Shipping Charges</h2>
              <p>
                Shipping charges are calculated transparently during checkout based on order subtotal:
              </p>
              <ul className="list-disc pl-5 space-y-1 text-xs">
                <li>Orders above ₹500 qualify for free delivery within the primary delivery region.</li>
                <li>Standard shipping charges apply for orders below ₹500 as shown on the order summary prior to payment.</li>
              </ul>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">4. Delivery Inspection</h2>
              <p>
                Customers are requested to inspect package integrity before accepting delivery from the courier agent. Contact us
                immediately at <strong>{BIZ_EMAIL}</strong> or <strong>{BIZ_PHONE}</strong> if a package arrives visibly damaged.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-base font-bold text-emerald-950 font-serif">5. Contact for Shipping Queries</h2>
              <p>
                <strong>Email:</strong> {BIZ_EMAIL}<br />
                <strong>Phone:</strong> {BIZ_PHONE}<br />
                <strong>Hours:</strong> {BIZ_HOURS}
              </p>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
};
