# warandpeace

A fandom wiki for *War and Peace*, built by an agent that reads the novel one chapter at a time.

It exists to demonstrate a failure everybody has already hit: ask a model to list every character in
a 365-chapter novel and you get Pierre, Natásha, Prince Andrew and a dozen others. Nothing fails,
nothing warns you, and you have no way to tell a fraction from the whole.

The fix is not a better prompt. It is a plan that partitions, a verifier the producer cannot
overrule, and a record that survives the run being killed — which is what
[ratchet](https://github.com/vasiliy-mikhailov/ratchet) is.

## The corpus

The Maude translation, public domain, Project Gutenberg #2600, split on the text's own markers with
the Gutenberg boilerplate stripped.

| | |
| --- | --- |
| chapters | 365 |
| words | 561,695 |
| books | 15, plus two epilogues |
| quoted passages | 8,958 |

The last number is why this works as a demonstration: a regex counts them, so **the model does not
get a vote on the denominator**.

## The page is the specification

Fandom's own guide for a character page prescribes the sections and puts `== Character arc ==`
first — *"the storyline of a character in an episode or season."* So the sections are not a
rendering step bolted on at the end. They are eight extraction questions asked of every chapter,
each a plan-do-verify triad with its own verifier:

| section | verify | strength |
| --- | --- | --- |
| `quotes` | the string occurs verbatim in the chapter | mechanical, exact |
| `appearances` | the name occurs; an arc entry exists | mechanical |
| `names` | the surface form occurs in the text | mechanical |
| `character-arc` | every entry anchored to a paragraph here | span-checkable |
| `relationships` | both parties real, the relation directional | span + referential |
| `facts` | status, rank, house as this chapter states them | span-checkable |
| `personality` | each trait cites a span that supports it | inference is judgement |
| `trivia` | true against a span — but *interesting* is not checkable | weakest |

## The shape

There is no diagram of this pipeline anywhere, deliberately. `Flow.shape` walks the tree the
runtime executes, so run `Picture` and the picture cannot be out of date:

```
character              character
    book                   fold
        chapter                book
            appearances            fold-book
            character-arc              character-arc … facts
            quotes                 fold-page
            relationships              character-arc … facts
            personality
            trivia
            names
            facts
```

Pass one is additive and nothing reconciles: a lane can stop between any two chapters and lose only
the chapter it was in. Pass two folds chapters into a book and books into a page — one operation at
two levels.

**The gates differ because the passes do.** Pass one's is *coverage*: 11,103 paragraphs, each read.
Pass two's cannot be preservation, because compaction's job is to lose things — it is *accounting*:
every reading reaches a recorded decision, kept or merged-into-X or dropped-because-Y.

## The gold standard

`gold/` holds 225 attributions over four chapters, from three independent passes adjudicated
against the text. They agreed on **97%**; the six they split on are marked `UNKNOWN` rather than
forced to a majority. That number is the ceiling — anything scoring above it is measuring noise.

It also records what a naive merge gets wrong. `Count Bezúkhov` denotes Pierre's *father* in every
occurrence in those chapters but one forecast; `Bolkónski` is the old prince or his son depending on
the sentence. Each is spelled out in a `not_variants` field, because a page that merges them is
confidently wrong, which is worse than one with a gap.

## Running it

An OpenAI-compatible endpoint. `.env` is gitignored; the launcher bridges its names to ratchet's.

```sh
./install-ratchet.sh          # ratchet from its source tree, into ~/.m2
mvn -o compile
java -cp target/classes:$(cat cp.txt) tech.mikhailov.wp.Picture   # print the shape
```

## Licence

The code is Apache 2.0. *War and Peace* in the Aylmer and Louise Maude translation is public
domain; Project Gutenberg's boilerplate is stripped and not redistributed. See `NOTICE`.
