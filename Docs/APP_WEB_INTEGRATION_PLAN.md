# HearIt Web Landing Page & Legal Integration Plan (`app-web`)

> **Repository Path**: `/Users/jayanthbharadwajm/development/app-web`  
> **Deployment Target**: Cloudflare Pages (`app-web`)  
> **Deploy Command**: `npx wrangler pages deploy public --project-name=app-web`  
> **Reference Subdomain / Path**: `https://<your-domain>/hearit` (matching `public/parkpin/`)  

---

## 1. Directory Structure Blueprint in `app-web`

Following the established pattern in `public/parkpin/`, HearIt will be added as a top-level product under `public/hearit/`:

```
/Users/jayanthbharadwajm/development/app-web/
├── public/
│   ├── app-ads.txt                  <-- Add HearIt AdMob Publisher ID line
│   ├── index.html                   <-- Add HearIt card in the multi-app portfolio
│   ├── images/
│   │   ├── hearit_icon_512.png
│   │   ├── hearit_hero_banner.png
│   │   └── hearit_feature_*.png
│   └── hearit/                      <-- Dedicated HearIt Product Website
│       ├── index.html               <-- Product Landing Page
│       ├── privacy/
│       │   └── index.html           <-- Google Play-Mandatory Privacy Policy
│       ├── terms/
│       │   └── index.html           <-- Terms of Service
│       └── support/
│           └── index.html           <-- FAQ, Setup Troubleshooting & Contact
```

---

## 2. Page Specifications & Content

### A. Landing Page (`public/hearit/index.html`)
- **Hero Section**:
  - Catchy Headline: *"Stay Connected, Keep Your Eyes on the Road."*
  - Subhead: *"Intelligent hands-free notification announcements for Bluetooth earphones, car audio, and Wear OS smartwatches."*
  - CTAs: *"Get it on Google Play"* badge + *"Explore Features"*.
  - Device Mockups: Side-by-side Phone showing HearIt Material 3 Expressive UI and Wear OS smartwatch showing notification card.
- **Feature Highlights Grid**:
  1. 🎧 **Universal Audio Routing**: Works over any Bluetooth earphones, wired headsets, or speaker.
  2. 🧠 **Smart On-Device NLP**: Cleans URLs, normalizes currency, and collapses spam emojis.
  3. ⌚ **Wear OS Companion**: Glanceable alerts and category filtering directly on your wrist.
  4. 📳 **Shake to Silence**: Intuitive gesture controls to stop speech or repeat recent messages.
  5. 🔒 **100% Private**: Zero cloud uploads; your personal notifications never leave your device.
- **User Guide & Video Embed / GIF**:
  - Simple 3-step setup: 1) Install HearIt, 2) Grant Notification Access, 3) Put in earphones and drive!

### B. Mandatory Privacy Policy (`public/hearit/privacy/index.html`)
Google Play strictly requires a live Privacy Policy URL in the Console before approving notification listener apps.
Key required disclosures:
- **Notification Access Disclosure**: Specifically explains that HearIt requires `BIND_NOTIFICATION_LISTENER_SERVICE` solely to read notification text aloud through the local TTS synthesizer and forward them to the user's paired Wear OS smartwatch.
- **Zero Remote Storage / Sale**: Explicitly guarantees that notification data is processed strictly in-memory on the device and is never stored on external servers, transmitted to third parties, or sold.
- **AdMob / Third-Party SDKs**: Discloses standard Google AdMob anonymized analytics/ad requests on the free tier.

### C. Terms of Service (`public/hearit/terms/index.html`)
- Safety disclaimer: Driver must observe local road laws and maintain safe driving conditions at all times.
- Subscription terms: Google Play Billing subscription auto-renewal and cancellation policies.

### D. Support & FAQ (`public/hearit/support/index.html`)
- Common questions:
  - *"Why are notifications not being announced?"* (Battery optimization / notification permission troubleshooting).
  - *"How do I connect my Wear OS watch?"* (Google Wearable DataLayer setup guide).
  - *"How do I mute speech when listening to music?"* (Music ducking & shake to stop).
- Direct developer contact email.

---

## 3. `app-ads.txt` Integration
In `/Users/jayanthbharadwajm/development/app-web/public/app-ads.txt`:
```text
google.com, pub-XXXXXXXXXXXXXXXX, DIRECT, f08c47fec0942fa0
```
This enables Google AdMob crawlers to verify the app authorization and maximizes ad fill rates.

---

## 4. Deployment Instructions

When ready to publish or update the web landing page:
```bash
cd /Users/jayanthbharadwajm/development/app-web
npx wrangler pages deploy public --project-name=app-web
```
The site deploys globally on Cloudflare Pages in less than 30 seconds!
