# Resolving the Great Undo-Redo Quandary

## First, Some History (technically a pun)

The Great Undo-Redo Quandary - the GURQ - happens when you're editing something (like an article about undo-redo), undo a ways, make some changes, and your redos go {{poof!}} because the editor doesn't know what else to do with them, so if you change your mind about changing your mind, you're stuck. It's a classic science _fiction_ problem: If you go back in time and change the past, you lose the future and you can't get it back. That's annoying, and that's the GURQ.

Every so often a random programmer will announce: "I've solved the GURQ!" First they decide that the problem happens because editors & word processors use a linear/stack-ish data structure for undo-redo when a _tree_ seems more appropriate. Of course that tree requires a navigation system for users to pick their way back through the undo-redo history, leading to all sorts of complicated user interface nonsense that nobody has time to deal with. Folks agree that it's a clever solution, just not worth bothering. So the problem goes back on the shelf for a few years until we rinse & repeat.

## There Is a Better Way

I actually solved the GURQ "the right way" back in the 1990's, as part of my own homemade editor(s) that I've used ever since. No, there is no "tree", nor any complicated graphical user interface to go with. In fact the user interface is the same as ever: You got your undos, your redos, and that's it. It's strictly linear, but all edit states are preserved and reachable, so whatever you're getting back to, it's either thisaway or thataway. I know this might sound implausible, but it makes perfectly good sense once you understand it, which I'll get to in a moment.

So as examples go, my current editor is named ["Klonk"](https://github.com/zaboople/klonk) (this page is hosted therein). It is kooky & homely as it should be, and you're welcome to download, build & run it so that you can see the GURQ-orithm in real time. Beyond that, you will despise Klonk for its kookiness and homeliness, but that's okay. (BTW, Klonk's predecessor was named "Severed Head in a Shopping Bag" - a strangely common phenomenon if you google it - and "Klonk" is just paying homage to the fact that SHSB had bit-rotted its way through the bottom)

Anyhow, I'll just refer to the GURQ-orithm as "Klonk" hereafter.

## How it worky?

With Klonk, when you try to go back in time and change the past, the future becomes... The past! Then if you change your mind and want to get "back to the future", you just start hitting that Undo button, and behold: You have your stuff back.

Huh?

Let me try again: If you just want to undo a ways, have a look-see, skedaddle like a bug and redo right back to "the present", that's okay; you're just window-shopping the undo/redo stacks, right? Things only get weird if you undo a ways, then squarsh the butterfly or kill the baby hitler; those undo actions now count as _changes_, as if you un-did the hard way, by remembering exactly how we got from baby hitler to now, and banging out those changes in reverse - so they're just plain old changes. Therefore, all of that lands in the undo stack.

So now "the present" looks just like 1889, and there you are, clutching dead baby hitler like some kind of psychotic freak with a bunch of Austrian nurses yelling at you - egads! You didn't think this through, and now you are completely shocked and disgusted with yourself, so what to do? Start smacking that undo button like mad. You'll go a few steps "backwards" through the murder, then zigzag "forwards", eventually coming to "now" if you keep at it.

At that point, the shameful death of baby hitler is where? Turns out it's in your redo stack, of course, because you un-did your way back "here".

So now you go on a drinking rampage, hoping to forget the horrors of what you've done, hoping even more that Klonk will forget, and... Zappo! Same thing: All that knifing/strangling/etc - as well as the act of traversing back to the opportunity - is instantly flung onto the undo stack at your first swig. Even after you wake up in a barf pizza on the bathroom floor the next morning, Klonk is still haunting you, because what happened, happened.

Just to be sure, you undo back through the binge, back to 1889 and doing the terrible deed, and indeed the deed is still done. Maybe now you just start hammering the undo button like mad, which will take you back to 2021, then undoing some more through the original post-hitlered history of the 20th century until 9 months before baby hitler to try killing his dad instead (!) because in your hazy morning stupor that somehow sounds like an improvement. You'll get there eventually, and if you pay attention, you'll see that you're just going backwards in the order of... what actually happened.

You could even change your mind yet again, maybe go after teenage hitler, who is a more reasonably deserving jerk, but enough of a hapless and unpopular doofus to be relatively murderable. So, undo thru dad's murder, the binge, baby hitler's murder, back to 2021 and then one more walk back to teenage hitler... Here it's worth pointing out that Klonk will actually stick to the zigs and skip over the zags so that you only have to watch yourself commit any previous crime exactly once, but maybe it shouldn't do that because you've become a crazed serial killer, now, haven't you?

Anyhow, wouldn't you know it, the mortally wounded teenage hitler manages to turn the knife back on you, and ouchie! You are bleeding rather profusely now! Hurry to the undo button! Before you...

----

_Note: Klonk will not actually stab you, no matter how bad of a writer/historian you are_

----

Essentially, changes are changes, not time travel. We've been travelling _forwards_ in time all along (duh) and Klonk is just tracking the history of Crazy Things We Did Today, in the order we did them.

So you can undo this way and redo that way and every which way, and Klonk will hang in there right behind you, like a creepy hacker groupie. It might get _complicated_, depending on how indecisive/capricious/bloodthirsty you are about things, but that's your problem. If you've been running yourself in circles, there's no hiding it. Klonk knows what you've been up to, and if you undo back to the very beginning and redo to the end, you'll see the history of your day (minus the window-shopping  and zags between zigs).

## Theoreticalisms

If you read the source carefully, or just think it over real hard, you'll suspect potential theoretical exponential stack growth; yes, because when baby hitler gets the business, Klonk makes a copy of the redo stack and flips that copy over before pushing both copy & original onto the undo stack. So if you do it exactly right, you can blow clean through the RAM roof in < 64 undo-redos, but this never becomes an issue in everyday editing.

## Concluderies

I know you think Klonk and even the GURQ-orithm is stupid and you're a little annoyed with me for pulling off a tricky dance move that seems out of my league, but is it bad? Beyond the minor memory disclaimer, really, no. It's just the same old undo/redo with a hidden enhancement, nothing new to learn, and if anything, it might pleasantly surprise you when you're able to get back the thing that you didn't expect to get back.

Again, I'm not trying to inculcate you into the Mystic Cult of Klonkism, nor am I even certain about first discovery of the GURQ-orithm. But maybe you could try teaching your own editor to do this lil' parlor trick and your users would be slightly happier for it.

## A Footknot

This is stupid, but... _Have_ I discovered time travel? No. Don't be silly.

In fact, let me ask: If I walk across town, am I still here? No, I'm _there_. So if you go back to 1984 looking for me, am I there? No. Nobody is in 1984. We and all the bits and pieces of us are _here_, in 20__. Even my hair is here, somewhere, hitchhiking I think. If you don't like travelling forwards in time like the rest of us, ok, but I think you're going to land in The Great Big Vvvt, and maybe you'll explode, or just get very bored.

It's basic thermodynamics: There's no free lunch, and certainly not thousands of lunch copies waiting for you to go re-eat each and every one until you come back and (this time for sure) explode into a diarrhea supernova of illegal lunches. The universe won't put up with it. So if you just wrote your hack science fiction comic book superhero plot into a corner, maybe just fix it instead of pulling out that time-travel nonsense again. Hopefully your word processor has decent undo/redo, like Klonk, because that part doesn't have to be bad science fiction.

