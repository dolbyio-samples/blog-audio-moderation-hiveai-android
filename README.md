# Blog post - Audio Moderation in conference using Hive.ai and the Dolby.io Web SDK

This repository contains a sample application for the blog post TODO ADD LINK. It allows you to use [Hive Audio Moderation](https://hivemoderation.com/audio-moderation) to capture raw local audio from Dolby.io conference, transcribe raw audio into human readable text, process the full transcription and detects inappropriate content across multiple classes.

## Requirements
If you do not have one already, create an account on Dolby.io and retrieve a client access token by connecting to your dashboard and selecting your application then click on API keys in the left menu.

![Client Access Token](images/dolby-io-access-token.png)

Then, create a [Hive](https://thehive.ai) account if you do not have one already and [Request a Demo](https://hivemoderation.com/contact-us?source=hivemoderation-header)

When you have access to to your dashboard, create a project and get your `Hive API Key`.

![Hive API Key](images/hive-api-key.png)

## Run the application

Open the project in Android Studio.
Update Configuration.kt with Dolby.io Client Access Token and Hive API Key.
Build the project and run it in a physical mobile device.