# Store screenshots

**Taken 5 Nov:** `00-welcome.png`, `01-menu.png`, `02-vetkoek.png`, `04-order-status.png`, `08-profile-settings.png`. They're shown in the main README.

Play needs at least 2 and takes up to 8: PNG or JPEG, 9:16, at least 1080 px
on the short side for the best placement. Take them on the phone, in light
mode, with the release APK. Save them here as `01-menu.png`, `02-vetkoek.png`
and so on.

Sign in as the test student first (`user123@gmail.com`), so the wallet shows
a balance and the order history isn't empty.

| # | Screen | What should be on it |
|---|---|---|
| 1 | **Menu** | The navy hero with the balance and the weather strip, category chips, rows with stock pills |
| 2 | **Vetkoek sheet** | 2 vetkoeks, two fillings, the live total on the button |
| 3 | **Cart** | Two lines, slot chips showing "12 of 40 taken" |
| 4 | **Order status** | The four code tiles and the progress rail with real times |
| 5 | **Push notification** | "Order #… is ready" on the lock screen or in the shade |
| 6 | **Staff queue** | Three orders in different states (sign in as `admin@gmail.com`) |
| 7 | **Sales** | Revenue cards and the orders-by-hour chart |
| 8 | **Settings in Afrikaans or Sesotho** | Shows the three languages |

Quick capture: phone screenshot (power + volume down), or with the phone
plugged in:

```
adb exec-out screencap -p > docs/play-store/01-menu.png
```
