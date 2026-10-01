# NoorPro Travel: partner setup

The Travel entry appears on Home and Explore. Flights supports adult passengers,
one-way and return trips, and all destinations through three-letter airport codes.
Umrah and Hajj have separate operator enquiry links. The existing pilgrimage guide
and planner remain available. This release does not issue tickets or take payments.

## Activate partners

1. Agree a commercial arrangement with a flight provider and suitable authorized
   pilgrimage operators, including support, refunds and any referral commission.
2. Review and deploy the `travelPartners` change in `firestore.rules` using the
   existing Firebase project deployment process. These rules have not been deployed.
3. Using a trusted administrator, create Firestore documents `travelPartners/flights`,
   `travelPartners/umrah`, and `travelPartners/hajj`. Each has:
   - `name`: the actual provider/operator name (string).
   - `urlTemplate`: an approved HTTPS booking or enquiry URL (string).
   - `enabled`: boolean; keep false until the agreement and link are verified.
4. Test links on a device. Check the displayed provider, correct destination and
   dates, one-way trips, round trips and passenger count. Confirm the provider's
   actual coverage of your departure countries and cancellation support.

Do not put credentials or API keys in these public documents. Writes require the
existing administrator custom claim. Configurations update through a Firestore
listener without an app release. Missing, disabled, malformed or unreadable
configurations leave booking unavailable and explain that status in the screen.

## Flight URL mapping

Use the provider's documented search URL format. Optional query placeholders are
`__ORIGIN__`, `__DESTINATION__`, `__DEPARTURE__`, `__RETURN__`, `__ADULTS__`, and
`__TRIP__`. Dates are YYYY-MM-DD, trip is `oneway` or `return`, and the return date
is empty for one-way trips. Values are URL encoded. Map these placeholders to
the provider's actual query parameters; these are not a universal booking API.
Use a plain landing-page URL if the provider has no supported search links; in
that case the user re-enters their journey on the partner site. Umrah and Hajj
URLs should be plain enquiry links without flight placeholders.

Before opening an external browser, NoorPro shows the provider and destination
host, and explains who handles payment, tickets, changes and refunds. No passport,
card or personal passenger data is collected or stored by this feature.

## Validation

`TravelBookingTest` covers invalid dates, reversed journeys, passenger limits,
airport codes, disabled/unsafe URLs and one-way link expansion.
Run `./gradlew :app:testDebugUnitTest :app:compileDebugKotlin` in an Android build
environment. Neither task could be run in the authoring environment because its
Gradle wrapper distribution was unavailable and network download was blocked.
Device layout, dark theme, keyboard/back behavior and Firestore permissions still
need verification before merging and publishing.
