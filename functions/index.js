const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();

exports.sendPlanetNotification = onDocumentCreated(
  "notifications/{token}/pending/{docId}",
  async (event) => {
    const token = event.params.token;
    const docId = event.params.docId;
    const data = event.data.data();

    if (!data || !data.title || !data.body) return;

    const message = {
      token: token,
      notification: {
        title: data.title,
        body: data.body,
      },
      data: {
        deepLink: data.deepLink ?? "",
      },
      android: {
        notification: {
          channelId: data.channelId ?? "channel_market",
          sound: "default",
        },
      },
    };

    try {
      await getMessaging().send(message);
    } catch (error) {
      console.error("FCM send error:", error);
    }

    await getFirestore()
      .collection("notifications")
      .doc(token)
      .collection("pending")
      .doc(docId)
      .delete();
  }
);
