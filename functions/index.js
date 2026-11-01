const { onCall, HttpsError } = require("firebase-functions/v2/https");
const stripe = require("stripe")("sk_test_51TQXZgEDqmYqyNRbDTkjjEzW5e5msfmhz7sa4OBKves4ajRQ4Qqm3CVaptqOUyrHCotOJYlU2mdg9Ms0m35MY2h700OFZM69kv");

exports.createPaymentIntent = onCall(async (request) => {
    const data = request.data;

    if (!data.amount) {
        throw new HttpsError("invalid-argument", "The function must be called with an 'amount'.");
    }

    try {
        console.log("Creating PaymentIntent for amount:", data.amount, "PKR");

        const paymentIntent = await stripe.paymentIntents.create({
            amount: Math.round(data.amount),
            currency: "pkr",
            automatic_payment_methods: { enabled: true },
        });

        console.log("PaymentIntent created successfully:", paymentIntent.id);

        return {
            clientSecret: paymentIntent.client_secret,
        };
    } catch (error) {
        console.error("Stripe Error:", error);
        throw new HttpsError("internal", error.message);
    }
});
