# Daily fitness

Open the existing workout logger and use the Daily Fitness card. Enter total session minutes (default 5) and body weight (default 70 kg, explicitly an example), then replace the draft with the preset. It loads one bodyweight set of 20 push-ups, 20 sit-ups, and 20 crunches. Edit/remove rows before completing the workout; saved sessions use the existing history, routines, and backup storage.

Total time is allocated across the three exercises, rather than counted three times. Approximate gross kcal = MET × 3.5 × kg ÷ 200 × minutes, rounded per exercise. Moderate push-ups/sit-ups use 3.8 MET; light crunches use 2.8 MET. Estimates include resting energy and do not infer calories from repetition count. Session time includes whatever breaks the user logs, so results are approximate. Editing each row's duration recalculates its estimate using the entered weight for this draft; saved calories remain editable. Weight is not persisted as session metadata.

Research:

- https://github.com/LibreFitOrg/LibreFit — Android Kotlin/Compose workout tracking with sets, reps, duration and local storage; useful architectural reference. No external source code copied or new dependency added.
- https://pacompendium.com/conditioning-exercise/ — 2024 Adult Compendium entries 02022 and 02024, used for the calorie assumptions.

Verification: DailyFitnessTest checks time allocation, bodyweight sets, validation, weight scaling, and invalid weight rejection. Existing workout tests cover persistence and multi-exercise aggregation.
