# Part 1: Research and design

Back to [[Ri-magwinya]] · [[The brief in plain English]]

**Submitted 24 Aug 2026. Mark: 90%.**

## What was asked

- One PDF, 10 MB max, with two documents inside.
- **Research report (50 marks).** Research 3 apps that work like ours, compare them, and pick their best features for our app.
- **Planning and design (50 marks).** Requirements covering every minimum feature, explained in our own words. The UI design. Which API we use, where it's hosted and which database. UML, including a use case diagram. A Gantt chart split into small tasks, with time for testing and bug fixing.
- 1000 to 1500 words per document, so the detail lives in tables.
- References in IIE Harvard style.

## What we handed in

- **The 3 apps:**
  - **CartZA:** pre-ordering at UFS Bloemfontein campus restaurants.
  - **Munchiez:** a UJ student-built ordering app with live tracking and student runners.
  - **LunchHub:** cashless meal booking for schools and workplaces.
- Requirements tables: main (MR-01 to MR-08), student (UR-01 to UR-13) and staff (SR-01 onwards).
- A REST API spec, a Gantt chart, and UML (use case, component, sequence, class).
- Mockups of all 11 screens (Figures 14 to 24).
- A clickable two-role HTML prototype. It's in the repo at `docs/rimagwinya-prototype.html`.
- The app was renamed from TuckShopOnCampus to Ri-magwinya partway through.

## Where Part 1 and the real app differ

Worth knowing in case the marker compares the design to the app.

- **Email.** Part 1 says college email only. The app takes any email, because students use Gmail. See [[Open decisions]].
- **Loyalty.** Part 1 says a free item on the 10th stamp. The app shows progress only. See [[Loyalty stamps]].
- **Staff notifications.** Part 1 says placing an order notifies staff. In the app, `order_placed` goes to the student as a confirmation, and staff only get `low_stock`. See [[Notifications]].

Each of these can be explained in the Part 3 write-up as a decision we made, rather than left for the marker to find.
