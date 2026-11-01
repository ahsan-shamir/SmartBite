package com.example.smartbitesample.data

import com.example.smartbitesample.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class EmailService {

    companion object {
        private const val SMTP_HOST = "smtp.gmail.com"
        private const val SMTP_PORT = "587"
        private val SENDER_EMAIL: String get() = BuildConfig.GMAIL_SENDER_EMAIL
        private val SENDER_PASSWORD: String get() = BuildConfig.GMAIL_APP_PASSWORD
        private const val SENDER_NAME = "SmartBite Official"

        fun create(): EmailService = EmailService()
    }

    suspend fun sendOtpEmail(toEmail: String, recipientName: String, otp: String) = withContext(Dispatchers.IO) {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", SMTP_HOST)
            put("mail.smtp.port", SMTP_PORT)
            put("mail.smtp.ssl.protocols", "TLSv1.2")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
            }
        })

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(SENDER_EMAIL, SENDER_NAME))
            setRecipient(
                Message.RecipientType.TO,
                InternetAddress(toEmail.trim(), recipientName.trim().ifEmpty { toEmail.trim() })
            )
            subject = "SmartBite - Your Verification Code: $otp"
            setContent(
                """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 12px;">
                    <h2 style="color: #FF7043; text-align: center; margin-bottom: 8px;">SmartBite</h2>
                    <p style="font-size: 16px; color: #333;">Hello <strong>$recipientName</strong>,</p>
                    <p style="font-size: 14px; color: #555;">Thank you for signing up with SmartBite! Use the verification code below to verify your account:</p>
                    <div style="text-align: center; margin: 24px 0;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #FF7043; background: #FFF3E0; padding: 12px 24px; border-radius: 8px; display: inline-block;">$otp</span>
                    </div>
                    <p style="font-size: 13px; color: #777; text-align: center;">This code will expire shortly. If you did not request this code, please ignore this email.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;" />
                    <p style="font-size: 12px; color: #999; text-align: center;">SmartBite Official &bull; Healthy Food Delivery</p>
                </div>
                """.trimIndent(),
                "text/html; charset=utf-8"
            )
        }

        Transport.send(message)
    }
}
