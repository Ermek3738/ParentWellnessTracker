const {onDocumentWritten} = require("firebase-functions/v2/firestore");
const {onSchedule} = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
admin.initializeApp();

// Health metrics thresholds
const THRESHOLDS = {
  heartRate: {
    high: 100,
    low: 50,
  },
  bloodPressure: {
    systolicHigh: 140,
    systolicLow: 90,
    diastolicHigh: 90,
    diastolicLow: 60,
  },
  bloodSugar: {
    high: 180,
    low: 70,
  },
};

// Function to analyze heart rate and send alerts if needed
exports.analyzeHeartRate = onDocumentWritten(
    "users/{userId}/healthData/heartRate",
    (event) => {
      const userId = event.params.userId;
      const data = event.data.after;

      // Skip if data was deleted
      if (!data) return null;

      const heartRate = data.data().value;
      const timestamp = data.data().timestamp;

      // Check if heart rate is outside normal range
      let alertType = null;
      if (heartRate > THRESHOLDS.heartRate.high) {
        alertType = "high_heart_rate";
      } else if (heartRate < THRESHOLDS.heartRate.low) {
        alertType = "low_heart_rate";
      }

      // If abnormal heart rate detected, send alert
      if (alertType) {
        return sendHealthAlert(
            userId,
            alertType,
            "Heart Rate",
            heartRate,
            timestamp,
        );
      }

      return null;
    });

// Function to analyze blood pressure
exports.analyzeBloodPressure = onDocumentWritten(
    "users/{userId}/healthData/bloodPressure",
    (event) => {
      const userId = event.params.userId;
      const data = event.data.after;

      if (!data) return null;

      const systolic = data.data().systolic;
      const diastolic = data.data().diastolic;
      const timestamp = data.data().timestamp;

      let alertType = null;
      if (systolic > THRESHOLDS.bloodPressure.systolicHigh ||
        diastolic > THRESHOLDS.bloodPressure.diastolicHigh) {
        alertType = "high_blood_pressure";
      } else if (systolic < THRESHOLDS.bloodPressure.systolicLow ||
               diastolic < THRESHOLDS.bloodPressure.diastolicLow) {
        alertType = "low_blood_pressure";
      }

      if (alertType) {
        return sendHealthAlert(
            userId,
            alertType,
            "Blood Pressure",
            `${systolic}/${diastolic}`,
            timestamp,
        );
      }

      return null;
    });

// Function to analyze blood sugar
exports.analyzeBloodSugar = onDocumentWritten(
    "users/{userId}/healthData/bloodSugar",
    (event) => {
      const userId = event.params.userId;
      const data = event.data.after;

      if (!data) return null;

      const bloodSugar = data.data().value;
      const timestamp = data.data().timestamp;

      let alertType = null;
      if (bloodSugar > THRESHOLDS.bloodSugar.high) {
        alertType = "high_blood_sugar";
      } else if (bloodSugar < THRESHOLDS.bloodSugar.low) {
        alertType = "low_blood_sugar";
      }

      if (alertType) {
        return sendHealthAlert(
            userId,
            alertType,
            "Blood Sugar",
            bloodSugar,
            timestamp,
        );
      }

      return null;
    });

/**
 * Sends health alerts to the user and their caregivers if applicable
 * @param {string} userId - User ID of the patient
 * @param {string} alertType - Type of health alert
 * @param {string} metricName - Name of the health metric
 * @param {any} value - The value of the health metric
 * @param {number} timestamp - Timestamp when the measurement was taken
 * @return {Promise<null>} Returns null after processing
 */
async function sendHealthAlert(userId, alertType, metricName, value, timestamp) {
  // Get user data to check if user has caregivers
  const userDoc = await admin.firestore().collection("users").doc(userId).get();
  const userData = userDoc.data();

  if (!userData) return null;

  // Create alert in Firestore
  const alertData = {
    userId: userId,
    type: alertType,
    metricName: metricName,
    value: value,
    timestamp: timestamp,
    read: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  };

  // Add alert to user's alerts collection
  await admin.firestore()
      .collection("users")
      .doc(userId)
      .collection("alerts")
      .add(alertData);

  // Send notification to user's device
  const userFCMToken = userData.fcmToken;
  if (userFCMToken) {
    let alertMessage = "";
    switch (alertType) {
      case "high_heart_rate":
        alertMessage = `High heart rate detected: ${value} BPM`;
        break;
      case "low_heart_rate":
        alertMessage = `Low heart rate detected: ${value} BPM`;
        break;
      case "high_blood_pressure":
        alertMessage = `High blood pressure detected: ${value} mmHg`;
        break;
      case "low_blood_pressure":
        alertMessage = `Low blood pressure detected: ${value} mmHg`;
        break;
      case "high_blood_sugar":
        alertMessage = `High blood sugar detected: ${value} mg/dL`;
        break;
      case "low_blood_sugar":
        alertMessage = `Low blood sugar detected: ${value} mg/dL`;
        break;
      default:
        alertMessage = `Health alert: ${metricName} at ${value}`;
    }

    const message = {
      notification: {
        title: "Health Alert",
        body: alertMessage,
      },
      token: userFCMToken,
    };

    await admin.messaging().send(message);
  }

  // If user has caregivers, send notifications to them as well
  if (userData.caregiverIds && userData.caregiverIds.length > 0) {
    for (const caregiverId of userData.caregiverIds) {
      const caregiverDoc = await admin.firestore()
          .collection("users")
          .doc(caregiverId)
          .get();
      const caregiverData = caregiverDoc.data();

      if (caregiverData && caregiverData.fcmToken) {
        const message = {
          notification: {
            title: `Health Alert for ${userData.fullName}`,
            body: `${userData.fullName}'s ${metricName.toLowerCase()} is ${value}`,
          },
          token: caregiverData.fcmToken,
        };

        try {
          await admin.messaging().send(message);
        } catch (error) {
          console.error("Error sending message to caregiver:", error);
        }
      }
    }
  }

  return null;
}

// Weekly health report generation function
exports.generateWeeklyHealthReport = onSchedule(
    {
      schedule: "every sunday 00:00",
      timeZone: "America/New_York",
    },
    async (event) => {
      // Get all users
      const usersSnapshot = await admin.firestore().collection("users").get();

      for (const userDoc of usersSnapshot.docs) {
        const userId = userDoc.id;
        const userData = userDoc.data();

        // Skip if user has no FCM token (can't receive notifications)
        if (!userData.fcmToken) continue;

        // Calculate date range for the past week
        const now = new Date();
        const oneWeekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);

        // Get health data for the past week
        const heartRateData = await admin.firestore()
            .collection("users")
            .doc(userId)
            .collection("healthData")
            .where("type", "==", "heartRate")
            .where("timestamp", ">=", oneWeekAgo.getTime())
            .orderBy("timestamp", "desc")
            .get();

        const bloodPressureData = await admin.firestore()
            .collection("users")
            .doc(userId)
            .collection("healthData")
            .where("type", "==", "bloodPressure")
            .where("timestamp", ">=", oneWeekAgo.getTime())
            .orderBy("timestamp", "desc")
            .get();

        const bloodSugarData = await admin.firestore()
            .collection("users")
            .doc(userId)
            .collection("healthData")
            .where("type", "==", "bloodSugar")
            .where("timestamp", ">=", oneWeekAgo.getTime())
            .orderBy("timestamp", "desc")
            .get();

        // Generate weekly report and save to Firestore
        const reportData = {
          userId: userId,
          generatedAt: admin.firestore.FieldValue.serverTimestamp(),
          period: "weekly",
          startDate: oneWeekAgo.getTime(),
          endDate: now.getTime(),
          heartRate: calculateMetricStats(
              heartRateData.docs.map((doc) => doc.data()),
          ),
          bloodPressure: calculateBloodPressureStats(
              bloodPressureData.docs.map((doc) => doc.data()),
          ),
          bloodSugar: calculateMetricStats(
              bloodSugarData.docs.map((doc) => doc.data()),
          ),
        };

        await admin.firestore()
            .collection("users")
            .doc(userId)
            .collection("reports")
            .add(reportData);

        // Send notification about new report
        if (userData.fcmToken) {
          const message = {
            notification: {
              title: "Weekly Health Report Available",
              body: "Your weekly health report is now available. Tap to view.",
            },
            token: userData.fcmToken,
          };

          try {
            await admin.messaging().send(message);
          } catch (error) {
            console.error("Error sending weekly report notification:", error);
          }
        }
      }

      return null;
    });

/**
 * Calculates statistics for a health metric from data points
 * @param {Array} dataPoints - Array of health data points
 * @return {Object} Statistics including count, average, min, max, and last values
 */
function calculateMetricStats(dataPoints) {
  if (!dataPoints || dataPoints.length === 0) {
    return {
      count: 0,
      avg: 0,
      min: 0,
      max: 0,
      lastValue: 0,
      lastTimestamp: 0,
    };
  }

  // If the metric has a specific value field, extract those values
  const values = dataPoints.map((dp) => dp.value || 0);

  return {
    count: values.length,
    avg: values.reduce((sum, val) => sum + val, 0) / values.length,
    min: Math.min(...values),
    max: Math.max(...values),
    lastValue: values[0],
    lastTimestamp: dataPoints[0].timestamp,
  };
}

/**
 * Calculates statistics for blood pressure from data points
 * @param {Array} dataPoints - Array of blood pressure data points
 * @return {Object} Statistics for both systolic and diastolic values
 */
function calculateBloodPressureStats(dataPoints) {
  if (!dataPoints || dataPoints.length === 0) {
    return {
      count: 0,
      avgSystolic: 0,
      minSystolic: 0,
      maxSystolic: 0,
      avgDiastolic: 0,
      minDiastolic: 0,
      maxDiastolic: 0,
      lastSystolic: 0,
      lastDiastolic: 0,
      lastTimestamp: 0,
    };
  }

  const systolicValues = dataPoints.map((dp) => dp.systolic || 0);
  const diastolicValues = dataPoints.map((dp) => dp.diastolic || 0);

  return {
    count: dataPoints.length,
    avgSystolic: systolicValues.reduce((sum, val) => sum + val, 0) /
        systolicValues.length,
    minSystolic: Math.min(...systolicValues),
    maxSystolic: Math.max(...systolicValues),
    avgDiastolic: diastolicValues.reduce((sum, val) => sum + val, 0) /
        diastolicValues.length,
    minDiastolic: Math.min(...diastolicValues),
    maxDiastolic: Math.max(...diastolicValues),
    lastSystolic: systolicValues[0],
    lastDiastolic: diastolicValues[0],
    lastTimestamp: dataPoints[0].timestamp,
  };
}
