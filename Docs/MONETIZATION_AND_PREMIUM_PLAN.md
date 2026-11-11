# HearIt Monetization, Banner Ads & Premium Gate Architecture

> **Document Type**: Architecture & Implementation Specification  
> **Target App**: HearIt (Phone & Wear OS)  
> **Reference Model**: ParkPin (`com.jayanth.parkpin`)  
> **Monetization Model**: Hybrid Freemium (Banner Ads + Temporary Pass via Rewarded Ads + Subscriptions / Lifetime IAP)  

---

## 1. Monetization Strategy & Value Proposition

HearIt follows a user-friendly **hybrid freemium model**:
1. **Core Utilities are 100% Free**: Basic notification speaking, essential app selection, shake gestures, and standard Bluetooth output are always free and functional.
2. **Advanced Intelligence & Power Features are Premium-Gated**: Smart NLP cleaning (URL summarization, currency normalization, emoji collapsing), Custom Spoken Names, Wear OS Watch Sync, and Emergency Breakthrough.
3. **Dual Path to Premium**:
   - **Zero-Cost Path (Rewarded Video Ads)**: Users can watch a short rewarded ad to unlock **12 Hours of Full Temporary Premium** (stackable up to 24 hours), including ad removal and smartwatch sync.
   - **Paid Path (Google Play Billing v7)**: Monthly subscription, discounted annual subscription, or lifetime single purchase.

---

## 2. Feature Tier Matrix (Free vs. Premium)

| Feature | Free Tier | Temporary Pass (Watch Ad) | Premium (Subscriber / Lifetime) |
| :--- | :---: | :---: | :---: |
| **Basic Spoken Notifications** | ✅ Unlimited | ✅ Unlimited | ✅ Unlimited |
| **App Allow / Block List** | ✅ Unlimited | ✅ Unlimited | ✅ Unlimited |
| **Bluetooth / Speaker Routing** | ✅ Full | ✅ Full | ✅ Full |
| **Shake / Flip to Silence** | ✅ Full | ✅ Full | ✅ Full |
| **Banner Ads** | ⚠️ Displayed (Bottom Sticky) | 🚫 Removed (0 Ads) | 🚫 Removed (0 Ads) |
| **Smart NLP Cleaning Engine** (URL Summarizer, Currency, Emojis) | 🔒 Locked | ✅ Unlocked (12h-24h) | ✅ Unlocked Forever |
| **Custom Spoken App Nicknames** (Idea 33) | 🔒 Locked (Max 1 app) | ✅ Unlocked | ✅ Unlocked Unlimited |
| **Wear OS Companion Watch App** | 🔒 Locked (Shows Preview) | ✅ Full Real-time Sync | ✅ Full Real-time Sync |
| **Periodic Spoken Check-ins** (Time & Battery) | 🔒 Locked | ✅ Unlocked | ✅ Unlocked |
| **Emergency Breakthrough Alerts** | 🔒 Locked | ✅ Unlocked | ✅ Unlocked |
| **Backup & Restore** | 🔒 Locked | ✅ Unlocked | ✅ Unlocked |

---

## 3. Architecture Overview

```
                                  +-----------------------------+
                                  |     Google Play Billing     |
                                  |   (Monthly, Annual, Life)   |
                                  +--------------+--------------+
                                                 |
                                                 v
+--------------------------+      +-----------------------------+
|    AdMob Rewarded Ad     | ---> |      AppPremiumManager      | <--- Local DataStore
|  (Grants 12h Temp Pass)  |      |   (Single Source of Truth)  |      (temp_expiry_ts)
+--------------------------+      +--------------+--------------+
                                                 |
                   +-----------------------------+-----------------------------+
                   |                                                           |
                   v                                                           v
     +---------------------------+                               +---------------------------+
     |        Phone App UI       |                               |    Wear OS Companion UI   |
     | • Gated Feature Guards    |                               | • Gated Watch Screen      |
     | • BannerAdView (AdMob)    |                               | • Synced Premium State    |
     | • 12h Pass Countdown      |                               | • No Ads on Watch         |
     +---------------------------+                               +---------------------------+
```

---

## 4. Temporary Premium ("Pass") Mechanics

Modeled directly after `com.jayanth.parkpin`:

1. **Duration**: 1 Rewarded Ad = **12 Hours of Full Premium Access**.
2. **Stacking Limit**: Maximum **24 Hours** stackable (watching a 2nd ad adds another 12 hours; button shows *"Pass Maxed"* if already >= 24h).
3. **Countdown Indicator**: In Settings and Home screens, an active temporary pass shows a live pill:
   - *"⚡ 12h Free Pass Active (11h 42m remaining)"* with a button to *"Add +12h"* or *"Upgrade to Permanent"*.
4. **Data Layer Cross-Device Synchronization**:
   - Phone broadcasts `temporary_premium_expiry_ts` to the Wear OS watch over Google Play Services DataLayer.
   - The watch reads the timestamp; if `now < expiry`, the watch automatically enables full notification sync with no phone prompt required.

---

## 5. Google Play Billing v7 SKU Setup

| SKU ID | Type | Recommended Price | Description |
| :--- | :--- | :--- | :--- |
| `hearit_premium_monthly` | Auto-renewing Subscription | ₹29 / \$0.99 / mo | Monthly unlimited access & ad removal |
| `hearit_premium_annual` | Auto-renewing Subscription | ₹149 / \$4.99 / yr | Annual access (over 50% discount) |
| `hearit_premium_lifetime` | In-App Non-Consumable | ₹399 / \$12.99 once | Permanent lifetime access & future updates |

---

## 6. Implementation Blueprint in HearIt Codebase

### Step 1: Storage Keys in DataStore (`core/datastore`)
In `core/datastore` or `HearItSettings.kt`:
```kotlin
val IS_PREMIUM_ENTITLED = booleanPreferencesKey("is_premium_entitled")
val TEMPORARY_PREMIUM_EXPIRY_TS = longPreferencesKey("temporary_premium_expiry_ts")
```

### Step 2: `AppPremiumManager` Singleton in `:app`
```kotlin
@Singleton
class AppPremiumManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    val isPremiumFlow: StateFlow<Boolean> = ...
    fun isPremium(): Boolean { ... }
    suspend fun grantTemporaryPremium(hours: Int = 12, maxStackHours: Int = 24) { ... }
}
```

### Step 3: Composable Premium Feature Gate wrapper
```kotlin
@Composable
fun PremiumGate(
    isPremium: Boolean,
    featureName: String,
    onUnlockClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (isPremium) {
        content()
    } else {
        LockedFeatureCard(
            featureName = featureName,
            onUnlock = onUnlockClick,
        )
    }
}
```

### Step 4: Sticky Banner Ad Composable (`BannerAdView`)
Using standard AdMob `AdView`:
```kotlin
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    isPremium: Boolean,
) {
    if (isPremium) return // Zero ads for premium & temporary pass holders!

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
```
