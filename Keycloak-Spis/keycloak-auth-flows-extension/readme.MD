🔐 Custom Keycloak Extensions – OTP & Conditional Authentication
📌 Overview

This repository contains a collection of custom Keycloak authenticators and conditional executors built on top of the Keycloak SPI.

These providers extend Keycloak’s authentication and registration capabilities with:

OTP enforcement during signup, browser login, and direct grant flows

Client-specific registration logic

Session-note–based conditional execution for flexible fallback scenarios

The project is designed for teams that need fine-grained control over user onboarding and secure login flows in Keycloak.

✨ Features

📲 Signup OTP Authenticator – verifies OTP before user creation

🌐 Browser Flow OTP Support – adds OTP verification into the standard browser login flow

🔑 Direct Grant OTP Authenticator – enforces OTP in Resource Owner Password Credential flows

👥 Conditional Client Authenticator – runs only for configured client(s)

📝 Session Note Condition Authenticator – makes decisions based on session notes set earlier in the flow

🛠 Custom Factories for easy configuration via the Keycloak Admin Console


⚡ Usage

After deployment, the new authenticators and conditions will appear in Admin Console → Authentication → Flows → Add execution:

Signup OTP Authenticator

OTP Authenticator (Browser Flow)

OTP Authenticator (Direct Grant)

Condition – Client (list)

Condition – Session Note

These can be combined to create custom registration/login flows.