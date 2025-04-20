package com.ermek.parentwellness.data.samsung.utils

import android.util.Log
import com.samsung.android.service.health.tracking.data.DataPoint
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Utility class for handling Samsung Health data points
 * Helps extract values from DataPoint objects safely
 */
object DataPointUtils {
    private const val TAG = "DataPointUtils"

    /**
     * Extract heart rate value from a DataPoint
     * Using multiple strategies to handle different SDK versions or implementations
     */
    fun extractHeartRate(dataPoint: DataPoint): Int {
        try {
            // Strategy 1: Try direct field access
            val heartRateValue = getFieldValue(dataPoint, "heartRate")
            if (heartRateValue is Number) {
                return heartRateValue.toInt()
            }

            // Strategy 2: Try getData method for array access
            val dataArray = getDataArray(dataPoint)
            if (dataArray != null && dataArray.isNotEmpty()) {
                return dataArray[0].toInt()
            }

            // Strategy 3: Try getHeartRate method if available
            val heartRateMethod = findMethod(dataPoint, "getHeartRate")
            if (heartRateMethod != null) {
                val result = heartRateMethod.invoke(dataPoint)
                if (result is Number) {
                    return result.toInt()
                }
            }

            // Log the DataPoint structure for debugging
            logDataPointStructure(dataPoint)

            // Return a fallback value
            return 0
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting heart rate: ${e.message}")
            return 0
        }
    }

    /**
     * Extract steps count from a DataPoint
     * Using multiple strategies to handle different SDK versions or implementations
     */
    fun extractStepCount(dataPoint: DataPoint): Int {
        try {
            // Strategy 1: Try direct field access (different possible field names)
            val stepFields = listOf("stepCount", "steps", "step")
            for (field in stepFields) {
                val stepValue = getFieldValue(dataPoint, field)
                if (stepValue is Number) {
                    return stepValue.toInt()
                }
            }

            // Strategy 2: Try getData method for array access
            val dataArray = getDataArray(dataPoint)
            if (dataArray != null && dataArray.isNotEmpty()) {
                return dataArray[0].toInt()
            }

            // Strategy 3: Try getter methods
            val stepMethods = listOf("getStepCount", "getSteps", "getStep")
            for (methodName in stepMethods) {
                val method = findMethod(dataPoint, methodName)
                if (method != null) {
                    val result = method.invoke(dataPoint)
                    if (result is Number) {
                        return result.toInt()
                    }
                }
            }

            // Log the DataPoint structure for debugging
            logDataPointStructure(dataPoint)

            // Return a fallback value
            return 0
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting step count: ${e.message}")
            return 0
        }
    }

    /**
     * Extract accuracy value from a DataPoint
     */
    fun extractAccuracy(dataPoint: DataPoint): Int {
        try {
            // Try direct field access
            val accuracyValue = getFieldValue(dataPoint, "accuracy")
            if (accuracyValue is Number) {
                return accuracyValue.toInt()
            }

            // Try getter method
            val accuracyMethod = findMethod(dataPoint, "getAccuracy")
            if (accuracyMethod != null) {
                val result = accuracyMethod.invoke(dataPoint)
                if (result is Number) {
                    return result.toInt()
                }
            }

            // Default to medium accuracy if not found
            return 2  // Medium accuracy
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting accuracy: ${e.message}")
            return 2  // Default to medium accuracy
        }
    }

    /**
     * Extract timestamp from a DataPoint
     */
    fun extractTimestamp(dataPoint: DataPoint): Long {
        try {
            // Use the DataPoint's timestamp field if available
            val timestamp = dataPoint.timestamp
            if (timestamp > 0) {
                return timestamp
            }

            // Try direct field access as fallback
            val timestampValue = getFieldValue(dataPoint, "timestamp")
            if (timestampValue is Number) {
                return timestampValue.toLong()
            }

            // Try getter method
            val timestampMethod = findMethod(dataPoint, "getTimestamp")
            if (timestampMethod != null) {
                val result = timestampMethod.invoke(dataPoint)
                if (result is Number) {
                    return result.toLong()
                }
            }

            // Return current time if not found
            return System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting timestamp: ${e.message}")
            return System.currentTimeMillis()
        }
    }

    /**
     * Get field value using reflection
     */
    private fun getFieldValue(obj: Any, fieldName: String): Any? {
        return try {
            val field = obj.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.get(obj)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Find method by name using reflection
     */
    private fun findMethod(obj: Any, methodName: String): Method? {
        return try {
            val method = obj.javaClass.getDeclaredMethod(methodName)
            method.isAccessible = true
            method
        } catch (e: Exception) {
            try {
                // Try with no parameters
                val method = obj.javaClass.getMethod(methodName)
                method.isAccessible = true
                method
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Get data array from getData method if available
     */
    private fun getDataArray(dataPoint: DataPoint): FloatArray? {
        return try {
            val getDataMethod = findMethod(dataPoint, "getData")
            if (getDataMethod != null) {
                val result = getDataMethod.invoke(dataPoint)
                result as? FloatArray
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Log the structure of a DataPoint for debugging
     */
    private fun logDataPointStructure(dataPoint: DataPoint) {
        try {
            Log.d(TAG, "DataPoint class: ${dataPoint.javaClass.name}")

            // Log fields
            Log.d(TAG, "Fields:")
            dataPoint.javaClass.declaredFields.forEach { field ->
                field.isAccessible = true
                try {
                    val value = field.get(dataPoint)
                    Log.d(TAG, "  ${field.name}: ${field.type} = $value")
                } catch (e: Exception) {
                    Log.d(TAG, "  ${field.name}: ${field.type} = [access error]")
                }
            }

            // Log methods
            Log.d(TAG, "Methods:")
            dataPoint.javaClass.declaredMethods.forEach { method ->
                if (method.parameterCount == 0) {
                    Log.d(TAG, "  ${method.name}(): ${method.returnType}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging DataPoint structure: ${e.message}")
        }
    }
}