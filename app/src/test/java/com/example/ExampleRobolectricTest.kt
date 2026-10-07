package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.sms.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Dipu Proxy Zone", appName)
  }

  @Test
  fun `test bkash sms parsing`() {
    val sms = "You have received Tk 500.00 from 01711000000. Fee Tk 0.00. Balance Tk 12,500.00. TrxID 9K8L2M3P01 at 07/10/2026 14:30"
    val parsed = SmsParser.parse("16247", sms)
    assertNotNull(parsed)
    assertEquals("bKash", parsed?.provider)
    assertEquals(500.0, parsed?.amount ?: 0.0, 0.01)
    assertEquals("9K8L2M3P01", parsed?.transactionId)
  }

  @Test
  fun `test nagad sms parsing`() {
    val sms = "Cash In Tk 1,200.00 from 01822000000. Fee Tk 0.00. Balance Tk 3,450.00. TxnID NG7812AB at 07/10/2026 15:10"
    val parsed = SmsParser.parse("16167", sms)
    assertNotNull(parsed)
    assertEquals("Nagad", parsed?.provider)
    assertEquals(1200.0, parsed?.amount ?: 0.0, 0.01)
    assertEquals("NG7812AB", parsed?.transactionId)
  }

  @Test
  fun `test unrelated otp sms ignored`() {
    val otpSms = "Your verification code is 492011. Do not share this with anyone."
    val parsed = SmsParser.parse("Google", otpSms)
    assertNull(parsed)
  }
}
