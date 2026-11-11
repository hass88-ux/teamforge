# Recommendation evaluation and feedback

Run from `ai`: `python -m unittest discover -s tests -v`, then `python -m teamforge_ai.challenge`.

The checked-in [challenge report](challenge-report.json) contains every case, label rationale, top choice and metric. Twelve authored two-candidate cases test complementary versus copied skills, unmet needs, one-sided intent, commitment, keyword stuffing, and timezone overlap. These related cases share a base profile; they are not independent human judgements or a representative outcome dataset.

The logistic baseline trains only on generated training profiles. Epoch selection uses generated validation profiles; challenge labels are not used for fitting or selecting epochs. Weighted, logistic, and a diagnostic availability-first ablation are compared using nDCG@2 and preferred-top-choice counts. The ablation was designed for this suite and is not evidence of general improvement. No learned model is promoted.

Two intent-exclusion cases have only one eligible candidate and test policy rather than ranking. The report also provides ranking-only nDCG excluding those cases; do not interpret the combined top-choice count as accuracy on twelve independent ranking problems.

| Method | Preferred top choice, all 12 cases | Ranking-only nDCG@2, 10 cases |
| --- | --- | --- |
| Weighted v2.1 | 11 | 0.963093 |
| Trained logistic baseline | 5 | 0.785621 |
| Diagnostic availability-first ablation | 12 | 1.000000 |

The remaining weighted failure favours a high-scoring person with no shared hours over a complementary person who does have shared hours. This scenario explicitly requires synchronous work. Whether that should be a hard constraint for actual users needs product research and genuine feedback; no availability hard filter is deployed here.

The suite exposed a timezone bug: choosing each profile's local calendar week independently could compare schedules seven days apart around UTC Monday. Weighted v2.1 uses one UTC week window, expands neighbouring local weeks for recurring Sunday/Monday hours, and excludes nonexistent DST hours. Weights and intent eligibility are unchanged. Regression tests cover these boundaries. Historical v2 evaluation reports are not directly comparable to v2.1 results.

## Genuine feedback collection

Members can optionally rate issued real recommendations Relevant or Not relevant. Ratings are separate from likes, matches and messages and do not currently change ranking or train a model. Demo/generated profiles produce no training labels.

The server issues opaque 30-minute feedback offers tied to the authenticated session and actor; at most 100 offers are retained per session. It stores one latest rating per actor/candidate pair, actual scorer version, compatibility, and seven numeric contributions. No profile prose, email, timezone, or selected schedule slots are copied into feedback records. Withdrawn, blocked, missing, fabricated and cross-session targets are rejected. Feedback is CSRF-protected, included only in its author's export, clearable in Account settings, and cascades when either account is deleted. There is no automatic retention schedule yet.

Labels will be selection-biased: only displayed candidates can be rated, repeat ratings overwrite previous ones, and non-ratings are not negatives. Before training, collect sufficient independently rated examples, review quality, define account-disjoint evaluation splits, and compare against the weighted baseline on untouched data. Neither this small suite nor initial feedback proves real-world matching success.
