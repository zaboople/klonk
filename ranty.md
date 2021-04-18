# The Great Undo-Redo Quandary

## Some history (technically, that's a pun)

Recently I saw some people talking about the agony and dissapointment of undo & redo on Hacker News, because someone had submitted a latest-greatest solution to The Great Undo-Redo Quandary. In case you don't know, the GURQ is when you're editing something (like an article about undo-redo) and you undo a ways, then start making changes, and this makes your redo stack go [poof!] because the editor doesn't know what else to do with it. So,  if you change your mind about changing your mind, you're stuck. You lost that part of your history and you can't ever get it back, and that's annoying. That's the GURQ.

Anyhow, the shocker comment of the day was roughly, "If something like this seems important to you, you have a problem. Just pay attention to what you're doing, for pete's sake." Something like that. This caused much dismay: You never change your mind, make a mistake, etc.? Ever? Dude, what are you, a god?

Let's rephrase the argument as: "Undo & redo are great in a pinch, but not when they're overcomplicated exercises in the excesses of user interface grandiosity, which is how most people try to fix the GURQ. Those tricks are more hassle than they're worth and I don't feel like bothering, cause I'm one of those simpler-is-better old-timer-beardy-tough-gal-guy types or whatever."

Ah! I know what you mean, you old-timer toughy type! So, I give you: Klonk. It has dirt simple undo, with a nigh-invisible twist.

(I tried to explain this within HN's comments, but this is sometimes like trying to have a conversation as a buffalo herd passes, and I got squarshed. So I'm going to make it all about me this time.)

Anyhow: Klonk?

## Yes, Klonk

[Klonk](https://github.com/zaboople/klonk) is a kooky and homely little pet editor that I made for myself. It's unremarkable, mostly. My friends even ridicule it, which is ok. For our purposes, the only thing that matters is Klonk's approach to undo/redo.

Klonk has a very unusual undo/redo _algorithm_. Mind you, the _user interface_ is nothing to write home about: You can "undo" and/or "redo". That's about as simple as you could ask for, and at first glance, it looks exactly the same as any other vanilla editor... Except it's _not_ the same! It's GURQ-proof.

All edit states are preserved! None are lost. No matter how many undos and redos you do, redoing your undone undos and undoing your redone redos and so forth, every single other state is reachable (eventually) from wherever you're at, by going either thisaway, or thataway - undo or redo.

Perhaps this seems impossible.

## How it worky

It's simple and confusing at the same time: With Klonk, when you try to go back in time - so to speak - and change the past, the future becomes... The past! Then if you change your mind and want to get "back to the future", you just start slapping that Undo button, and behold: You got your stuff back.

Huh?

Let me try again: If you just want to undo a ways, have a look-see, skedaddle like a bug and redo right back to "the present", that's okay; you're just window-shopping the undo & redo stacks, right? BUT if you undo a ways, then squarsh the butterfly or kill the baby hitler or something, those undo actions now count as EDITS, or CHANGES, as if you un-did the hard way, by remembering exactly how we got from baby hitler to now, and banging out those changes in reverse, as plain old changes. Therefore, all of that lands in the undo stack.

So now "the present" looks just like 1889, and soon, there you are, clutching dead baby hitler like some kind of psychotic freak with a bunch of Austrian nurses screaming at you! Egads. You didn't think this through, and now you are completely shocked and disgusted with yourself, so what to do? Start smackin' that undo button. You'll go a few steps "backwards", then zigzag "forwards", eventually coming to 2021 if you keep at it.

At that point, the shameful death of baby hitler is where? It's in your redo stack, of course, because you un-did your way back "here".

So now you go on a drinking rampage, hoping to forget the horrors of what you've done, hoping even more that Klonk will forget, and... Zappo! Same thing: All that knifing and strangling - as well as the act of traversing back to the opportunity - is instantly flung onto the undo stack at your first swig. Even after you wake up in a puddle of vomit on the bathroom floor the next morning, Klonk is still haunting you, because what happened, happened (this is why I quit drinking again).

Just to be sure you did the unthinkable, you undo back through the binge, back to 1889 and doing the terrible deed, and indeed the deed is still done. Maybe now you just start hammering that undo button like mad, which will take you back to 2021, then undoing some more through the original hitlered history of the 20th century until 9 months before baby hitler to try killing his dad instead (!) because in your hazy morning stupor that somehow sounds like an improvement. It will be a long, weird journey, but you'll get there (poor geezer, and god help you, you're a double murderer now), and if you watch carefully, you'll see that you're just going backwards in the order of... what actually happened.

You could even change your mind yet again, maybe go after teenage hitler, who is a more reasonably deserving jerk, but still enough of a hapless, unpopular doofus to be easily murderable. So, undo thru dad's murder, the binge, baby hitler's murder, back to 2021 and then one more walk back to teenage hitler... Klonk will actually skip some over the zig-zagging to make it easier to move quickly, but maybe it shouldn't because you've become a crazed serial killer, now, haven't you?

Anyhow, wouldn't you know it, the mortally wounded teenage hitler manages to turn the knife back on you, and ouchie poo! You are bleeding rather profusely now! Hurry! To the undo button! Before you lose consciousness...

----

(Note: Klonk will not actually stab you, no matter how bad of a writer/historian you are)

----

Essentially, changes are changes. We've actually been travelling _forwards_ in time all along (duh) and Klonk is just tracking the history of Crazy Awful Things I Did Today. We're treating "undo" as changes _instead of_ time travel.

Anyhow, in summary, you can undo this way and redo that way and every which way, and Klonk will hang in there right behind you, like a creepy hacker groupie. It might get _complicated_, depending on how impulsive and mercurial you are about things, but that's your problem. If you've been running yourself in circles, there's no hiding it. Klonk knows what you've been up to.

## Theoreticalisms

If you read the source carefully, or just think it over real hard, you'll suspect theoretical exponential stack growth; yes, it's even a little worse, because when baby hitler gets the business, Klonk makes a copy of the redo stack and flips that copy over before pushing both copy & original onto the undo stack. This never becomes an issue in everyday editing, but it might if adapted to, say, database redo logs, version control systems and some of the people I work with (no further comment).

Also, I know it might seem better to substitute Klonk's stacks with tree-like graphy thingies. I tried that but never got anywhere with it.

## Concluderies

I know you still think Klonk is stupid and you're a little annoyed with me for pulling off a tricky dance move that seems out of my league, but is it bad? Beyond the previous memory disclaimers, really, no. It's just the same old undo/redo with a hidden enhancement. Nothing new to learn, and if anything, it might pleasantly surprise you when you're able to get back the thing that you didn't expect to get back.

Anyhow, I'm not trying to inculcate you into the Mystic Cult of Klonkism, nor am I even claiming first discovery of this algorithm. I'm just saying, hey, here's another way to think about the problem, and thinking is useful (unless you're at work and your boss is reminding you that you're not paid to think, you're paid to move that login button two pixels to the right because it was decided in yesterday's meeting that four to the left was too far).

## A Footknot

This is stupid, but... _Have_ I discovered time travel? No. Don't be silly.

In fact, let me ask: If I walk across town, am I still here? No, I'm _there_. So if you go back to 1984 looking for me, am I there? No. Nobody is in 1984. We and all the bits and pieces of us are _here_, in 2021. Even my hair is here, somewhere, hitchhiking I think. If you don't like travelling forwards in time like the rest of us, ok, but I think you're going to land in The Great Big Vvvt, and maybe you'll explode, or just get bored.

It's basic thermodynamics: There's no free lunch, and certainly not thousands of lunch copies waiting for you to go re-eat each and every one until you come back and (this time for sure) explode into a diarrhea supernova of illegal lunches. The universe won't put up with it. So if you just wrote your hack science fiction comic book superhero plot into a corner, maybe just fix it instead of pulling out that time-travel nonsense again. Hopefully your word processor has decent undo/redo, like Klonk, because that part doesn't have to be bad science fiction.

