# Resolving the Great Undo-Redo Quandary

## The History (technically a pun)

The Great Undo-Redo Quandary - the GURQ - happens when you're editing something (like an article about undo-redo), undo a ways, make some changes, and your redos go {{{poof!}}} because the editor doesn't know what else to do with them, so if you change your mind about changing your mind, you're stuck. It's a classic science _fiction_ problem: If you go back in time and change the past, you lose the future and can't get it back. That's annoying, and that's the GURQ.

Every so often a random programmer will announce: "I've solved the GURQ!" First they decide that the problem happens because editors & word processors use a linear/stack-ish data structure for undo-redo when a _tree_ seems more appropriate. Of course that tree requires a navigation system for users to pick their way back through the undo-redo history, leading to all sorts of complicated user interfacery that nobody has time to deal with. Folks agree that it's a clever solution, just not worth bothering. So the problem goes back on the shelf for a few years until someone re-discovers the same idea. It's overdue for someone to say, "Please stop doing this!"

## There Is a Better Way

I learned how to resolve the GURQ "the right way" (opinion) back in the 1990's and use it in my own homemade editor(s). No, there is no undo "tree", nor any complicated graphical user interface to go with. In fact the user interface is the same as ever: You've got your undos, your redos, and that's it. The underlying data structure is strictly linear, but all edit states are preserved and reachable, so whatever you're getting back to, it's either thisaway or thataway (usually thisaway). There's no need for users to learn a new featureset for the sake of this "enhancement", and most won't notice the difference until... hey, how about that! All edit states are recoverable.

This might sound implausible at first.

As examples go, my current editor is named ["Klonk"](./..). It is kooky & homely as it should be, and you're welcome to download, build & run it so that you can see the GURQ-orithm in real time. Beyond that, you will despise Klonk for its kookiness and homeliness, but that's okay. (Klonk's predecessor was named "Severed Head in a Shopping Bag" - a strangely common phenomenon according to the internet - and "Klonk" is just paying homage to the fact that SHSB had bit-rotted its way through the bottom)

## How Does It Work?

Suppose that you've undone & redone yourself into a corner - the GURQ has struck again. But suppose again, that someone leans in - a "Clippy" sort of character - and says, "Don't worry! I've quietly recorded everything you've done today! All of your changes are just a perfectly linear history with respect to time." So you agree to "rewind" through the history of today back to a stopping point where you're happy. Ta-da: You've got what you wanted.

Now suppose you've changed your mind about changing your mind: Clippy leans in again, and says, "Don't worry!" At this point you might protest, "But we already did that! We can't go back on going back!" But Clippy points out that even including the "rewind" you previously did, you *still* have a perfectly linear history with respect to time. So you simply rewind again, and there you go. Computers don't violate physics.

So, we need only implement this Clippy-ish thing and we have a way out.

## Making it simpler

First of all, there's no need for undo/redo as well as this "meta undo/redo". We can merge the two into one.

Second of all, there's no need to include "window shopping" in our linear history; this is when you undo a ways, take a look around, then skedaddle out of there back to "the present" without changing anything.

The act of undoing need only become part of our linear history IF we squash the proverbial butterfly and thus alter "the past"; in that case the act of undoing itself is instantly be recorded as a series of changes, by replaying the redo stack *onto* the undo stack *twice*: 1) Forwards, for the original changes 2) and then backwards, to record the history of our undoing (we'll come back to the memory space usage). So:

    A) ----> Original changes
    B) <---- Undoing them
    C) --->  New changes

becomes linearly:

    A) ----> B) ----> C) --->

After C) is done, this A->B->C progression is a perfectly linear history in its own right, so we can rinse and repeat the process ad nauseum. We can recover any previous state by stepping backwards carefully and then start on some new edits from there; a new A-B-C switcharoo happens again accordingly. Overall, the effect is that after making any change, all previous states are *somewhere* in the undo stack, i.e. our linear history, where they logically ought to be. Whatever you're typing right now, everything else is history.

Third, although it isn't strictly necessary, we can add a slight shortcut because of redundancies. In the A-B-C example above, B) is redundant with A), because one is just the other in reverse. When the butterfly is squashed and we create B, we mark its beginning and end as "instant"; whenever we later traverse onto *either* end of B), we proceed to the opposite end in one fell swoop to save the user some traversal time. All edit states are recoverable from A & C alone. Again, it's strictly optional, but less cumbersome.

## But Nobody Can Understand That!

As a programmer, you might have to perform a tiny bit of mental gymnastics to get all this in place, but as a user, if you undo and redo backwards and forwards (remember, these are your only options) you will see the history of your efforts playing back and forth *as they happened*. The user's reference point is history itself, because the algorithm is NOT "time-travelling" in any sense of the phrase; it is simply making changes to get to a desired state with respect to a perfectly linear passage of time.

A particularly capricious user might still be perplexed when walking back through their changing-their-mind-about-changing-their-mind-about-etc's, and even give up the hunt. Still, they aren't *losing* anything due to the GURQ-orithm; compared to the classical throw-history-away design, they are getting a bonus that they will eventually discover and muddle through as they like. There isn't a particular need to document it other than for completeness and bragging rights.

## Back to Memory Space Usage

Again, the act of going back and squashing the proverbial butterfly in history has a "doubling" effect, where you dump two copies of the redo stack onto the undo stack, rather than 1. If you know exactly what you're doing, you can use up all available memory in approximately 64 quick steps. In practice, this is unlikely; I've never done it by accident in decades of everyday use, because I'm not *quite* so mentally unstable. But if you were inspired to use the GURQ-orithm at a larger scale - in databases, for example - you'd want to be careful about this.

## Git already has this feature

Git has a "revert" command that backs out individual commits by creating *new* commits that reverse them. Can you revert a revert? Of course! All of this just becomes linear history. It's an excellent way to bail out of a production failure resulting from your latest brilliant idea, and then set about to fixing the wrong parts of that idea.

In fact Git allows you to skip over commits and attempt to revert others further back in history (obviously this can cause a "merge conflict"). We won't recommend this kind of "skipping" in a text editor because it brings us back to overly complicated features that nobody has time for, and after all: We already have version control software. We're just trying to make undo/redo a little better.

## Conclusion

Again, all of this is built into Klonk, but Klonk is just for me. You can implement the GURQ-orithm in your own editor with minor effort, and your users will be slightly better off for it, while losing nothing. I only recommend that you consider making the small effort necessary.
