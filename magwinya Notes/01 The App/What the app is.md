# What the app is

Back to [[Ri-magwinya]]

## The short version

Ri-magwinya is a pre-ordering app for the tuckshop at Rosebank College,
Bloemfontein. You order on your phone during class, pay from a wallet you
topped up earlier, and at break you walk up, say four digits, and take your
food. Staff see every order land on a screen and work through them in order.

## The problem

Break is twenty minutes. The queue is the whole twenty minutes. By the time
you're at the front, either you're late for the next class or the vetkoek is
finished. And the person behind the counter is trying to take an order, make
it, and run a card machine at the same time.

So the app pulls three things out of that twenty minutes:

- **The ordering** happens before break, from wherever you are.
- **The paying** happens whenever you top up, not in the queue.
- **The deciding** happens while you're sitting down with the menu in front
  of you, not while six people behind you sigh.

What's left at the counter is handing over a bag. That's it.

## What makes it more than a form

A few things push it past "a menu with a submit button":

- **Stock is real.** If there are four Score energies left, the app says four
  left, and when the fourth one sells the item goes sold out on everyone's
  phone at once. See [[Stock]].
- **Breaks have capacity.** Only so many orders can be collected in twenty
  minutes, so each slot fills up and closes. See [[Collection slots]].
- **The food is configurable.** Vetkoek with two polony and a cheese is a
  different thing, at a different price, from a plain vetkoek. See
  [[Building an order]].
- **The weather nudges the menu.** Cold and raining, the teas and coffees
  float to the top. See [[Weather]].
- **It works with no signal.** Orders placed offline get sent the moment the
  phone reconnects. See [[Offline mode]].

## What it deliberately isn't

- It doesn't touch card details. Ever. Topping up happens on the tuckshop's
  own speed point, and staff just type the amount into the app afterwards.
  The two systems are separate on purpose. See [[The campus wallet]].
- It isn't a delivery app. Nothing gets brought to you.
- It isn't a loyalty scheme pretending to be a food app. The stamps are a
  small extra. See [[Loyalty stamps]].

## Where the design comes from

There's a working HTML prototype (`docs/rimagwinya-prototype.html`) that shows
every screen, every bit of copy and every interaction. That's the visual
source of truth. The brief in `CLAUDE.md` overrides it wherever the two
disagree, and [[The rules that matter]] lists the places they disagree.
