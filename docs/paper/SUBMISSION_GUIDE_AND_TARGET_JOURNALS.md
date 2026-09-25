# Submission Guide & Journal Strategy for the Ether Academic Paper

**Paper Title**: *Thermodynamic Grounding and Empirical Falsification in Planetary Cliodynamics: The Architecture of the Ether Simulation Engine*  
**Lead Author**: Silvere Martin-Michiellot  
**Date**: September 2026  
**Document**: Submission Guidelines, Journal Targets, Peer Reviewer Nominations & Editorial Cover Letter Template  

---

## 1. Executive Summary & Publishing Strategy

The Ether research manuscript bridges **computational physics**, **non-equilibrium thermodynamics**, **cliodynamics**, and **computational social sciences**. Because it introduces both a novel discrete global simulation platform (Uber H3 DGGS) and empirical falsification results across multi-millennial datasets (Seshat, Maddison, HYDE 3.4), the manuscript can target top-tier interdisciplinary journals as well as specialized computational history and complex systems journals.

---

## 2. Targeted Peer-Reviewed Journals Ranking

```
╔══════════════════════════════════════════════════════════════╦═══════════════╦══════════════════════════╦═════════════════════════════════════════════════╗
║ Journal & Publisher                                          ║ Impact Factor ║ Scope / Focus Match      ║ Submission Track / Recommendation               ║
╠══════════════════════════════════════════════════════════════╬═══════════════╬══════════════════════════╬═════════════════════════════════════════════════╣
║ 1. Nature Computational Science (Nature Portfolio)           ║ 11.3 (Q1)     ║ Interdisciplinary HPC,   ║ PRIMARY TARGET (High Impact)                    ║
║                                                              ║               ║ Earth systems & societal ║ Fast-track research article on epistemic        ║
║                                                              ║               ║ simulation.              ║ falsification and thermodynamic grounding.      ║
╠══════════════════════════════════════════════════════════════╬═══════════════╬══════════════════════════╬═════════════════════════════════════════════════╣
║ 2. Cliodynamics: The Journal of Quantitative History (eScholar)║ 3.8 (Q1)    ║ Core cliodynamics,       ║ DOMAIN-SPECIFIC BENCHMARK                       ║
║                                                              ║               ║ Seshat, secular cycles.  ║ Ideal for comprehensive mathematical review by  ║
║                                                              ║               ║                          ║ Turchin, Goldstone, and cultural evolutionists. ║
╠══════════════════════════════════════════════════════════════╬═══════════════╬══════════════════════════╬═════════════════════════════════════════════════╣
║ 3. Journal of Artificial Societies and Social Simulation (JASSS)║ 4.2 (Q1)   ║ Agent-based & continuum  ║ METHODOLOGICAL BENCHMARK                        ║
║                                                              ║               ║ simulation architectures.║ Diamond Open Access (no APC fee).               ║
╠══════════════════════════════════════════════════════════════╬═══════════════╬══════════════════════════╬═════════════════════════════════════════════════╣
║ 4. PNAS (National Academy of Sciences, USA)                  ║ 11.1 (Q1)     ║ Complex adaptive systems,║ HIGH-PROFILE INTERDISCIPLINARY                  ║
║                                                              ║               ║ historical dynamics.     ║ Requires condensed main text (6 pages) +        ║
║                                                              ║               ║                          ║ extensive Supplementary Information.            ║
╠══════════════════════════════════════════════════════════════╬═══════════════╬══════════════════════════╬═════════════════════════════════════════════════╣
║ 5. Technological Forecasting & Social Change (Elsevier)      ║ 12.9 (Q1)     ║ Long-term tech change,   ║ EXCELLENT FIT FOR EXERGY / MACRO-HISTORY        ║
║                                                              ║               ║ biophysical constraints. ║ Strong focus on Smil exergy vs neoclassical.    ║
╚══════════════════════════════════════════════════════════════╩═══════════════╩══════════════════════════╩═════════════════════════════════════════════════╝
```

---

## 3. Pre-Submission Protocol & Artifact Preparation

Before submitting to the chosen journal's portal (e.g., Editorial Manager, ScholarOne, or Nature Springer Nature Manuscript Central):

### Step 1: Pre-print Deposit
Deposit the manuscript on open-access pre-print servers to secure academic precedence:
* **SocArXiv / OSF Preprints** (Domain: Cliodynamics, Economic History, Social Science).
* **arXiv.org** (Category: `physics.soc-ph` - Physics and Society, or `cs.MA` - Multi-Agent Systems).
* Generate a persistent DOI (Digital Object Identifier) for citation in early conference presentations.

### Step 2: Code & Data Repository Archive
Top journals strictly mandate reproducible code and data availability:
* Tag the exact release version of the GitHub repository: `v1.0.0-falsification-paper`.
* Archive the source code, tests, and benchmark JSON on **Zenodo** ([https://zenodo.org](https://zenodo.org)) to obtain an immutable software DOI (e.g. `10.5281/zenodo.XXXXXXX`).
* Ensure all benchmark datasets (`historical_cliodynamic_benchmarks.json`, Seshat data loaders, HYDE 3.4 scripts) are referenced in the *Data Availability Statement*.

### Step 3: Formatting & LaTeX Conversion (if required)
* The manuscript is currently in standard GitHub Flavored Academic Markdown.
* If targeting *Nature Computational Science* or *PNAS*, compile to LaTeX using the journal's official template (e.g., `nature.cls` or `pnas-new.cls`) via Pandoc:
  ```bash
  pandoc ACADEMIC_PAPER_PLANETARY_CLIODYNAMICS_FALSIFICATION.md -o manuscript.tex --template=nature_template.tex --citeproc
  ```

---

## 4. Nominated Peer Reviewers (Suggested Referees)

When submitting, journals request 4 to 6 nominated referees with relevant expertise:

1. **Prof. Peter Turchin** (Complexity Science Hub Vienna / University of Connecticut)
   * *Expertise*: Cliodynamics, Structural-Demographic Theory (SDT), Seshat Global History Databank.
   * *Relevance*: Direct evaluator of SDT, secular cycles, and quantitative historical falsification.
2. **Prof. Walter Scheidel** (Stanford University, Department of Classics & History)
   * *Expertise*: Inequality dynamics, Great Leveler thesis, ancient state collapse, Roman demography.
   * *Relevance*: Evaluator of wealth inequality (Gini), epidemic shocks, and Malthusian boundary dynamics.
3. **Prof. Timothy M. Lenton** (Global Systems Institute, University of Exeter)
   * *Expertise*: Earth System Science, AMOC tipping points, planetary boundaries, biogeochemical feedbacks.
   * *Relevance*: Expert reviewer for Tier 1 biophysical invariants, Stommel AMOC, and climate-society coupling.
4. **Prof. Daron Acemoglu** (Massachusetts Institute of Technology - MIT)
   * *Expertise*: Institutional economics, extractive vs inclusive property rights, long-run growth divergence.
   * *Relevance*: Critical evaluator of the institutional vs geographic falsification benchmarks.
5. **Prof. Vaclav Smil** (University of Manitoba, Fellow of the Royal Society of Canada)
   * *Expertise*: Energy transitions, biophysical economics, material throughput, agricultural nitrogen stoichiometry.
   * *Relevance*: Evaluator of the exergy, EROEI, and Liebig soil nutrient minimum formulations.

---

## 5. Formal Editorial Cover Letter Template

```text
To: The Editor-in-Chief
Journal: Nature Computational Science / Cliodynamics / JASSS

Subject: Submission of Original Research Article - "Thermodynamic Grounding and Empirical Falsification in Planetary Cliodynamics: The Architecture of the Ether Simulation Engine"

Dear Editor,

Please find enclosed our original research manuscript titled "Thermodynamic Grounding and Empirical Falsification in Planetary Cliodynamics: The Architecture of the Ether Simulation Engine", which we submit for consideration as an Original Research Article.

Macro-historical simulation has long struggled with a fundamental epistemological tension between qualitative narrative constructs and unconstrained econometric regressions ("curve fitting"). Too often, models that fail to match empirical trajectories are ad-hoc adjusted with dozens of uncalibrated parameters, creating the illusion of predictive validity without genuine scientific falsifiability.

To address this crisis, our paper introduces Ether, an open-source, discrete global hexagonal (Uber H3) planetary simulation platform grounded in non-equilibrium thermodynamics and quantitative cliodynamics. Ether establishes a strict Two-Tier Ontological Separation:
1. Tier 1 (Core Invariant Physics): Non-negotiable conservation laws of energy, mass, exergy, soil stoichiometry (Liebig N-P-K), Stommel AMOC circulation, and wet-bulb lethality (Stull).
2. Tier 2 (Pluggable Cliodynamic Hypotheses): Contested sociological and institutional theories (Boserup vs. Malthus, Turchin SDT vs. Pinker, Smil Exergy vs. Nordhaus DICE, Ostrom vs. Hardin, Acemoglu vs. Geography) formalized as coupled differential equations and tested using Approximate Bayesian Computation (ABC-SMC).

Benchmarked across deep historical time (-100,000 BP to present) against the Seshat Global History Databank, the Maddison Project Database (2020), and HYDE 3.4, our manuscript:
- Demonstrates that sociological hypotheses hold only within strictly bounded thermodynamic envelopes;
- Evaluates 20 procedural engines with rigorous effect sizes (|d| >= 0.80) and empirical R² fits;
- Provides an unvarnished audit of model limitations and a five-year computational roadmap.

All source code, automated falsification suites, and benchmark datasets are open-source and archived with a persistent DOI on Zenodo, ensuring 100% bit-identical reproducibility under strict IEEE-754 deterministic conditions.

Given the broad interdisciplinary interest in complex systems, biophysical economics, and computational history, we believe this manuscript will resonate deeply with your readership.

This manuscript is original work, has not been published elsewhere, and is not currently under consideration by any other journal. All authors have approved the submission.

Thank you for your time and consideration.

Sincerely,

Silvere Martin-Michiellot
Lead Developer & Corresponding Author
Department of Computational History & Cliodynamics, Ether Research Initiative
Email: [silvere.martin-michiellot@ether-sim.org]
```
