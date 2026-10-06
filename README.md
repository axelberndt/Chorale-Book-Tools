# Chorale Book Tools

[![GitHub release](https://img.shields.io/github/v/release/axelberndt/Chorale-Book-Tools)](https://github.com/axelberndt/Chorale-Book-Tools/releases/latest)
[![documentation](https://img.shields.io/badge/doc-JavaDoc-green.svg)](http://axelberndt.github.io/Chorale-Book-Tools/)
[![GPL v3](https://img.shields.io/github/license/cemfi/meico.svg)](https://github.com/cemfi/meico/blob/master/LICENSE)
![Java compatibility](https://img.shields.io/badge/Java-1.8+-blue)

Author: [Axel Berndt](https://github.com/axelberndt) ([Paderborn University](https://www.muwi-detmold-paderborn.de/personen/professorinnen-und-professoren/prof-dr-ing-axel-berndt), Detmold)

This collection of Java classes and functionality is used to analyze MEI encodings of chorale books such as the [Anhaltisches Choralbuch](https://github.com/axelberndt/Anhaltisches-Choralbuch-digital) and the [Neues Thüringer Choralbuch](https://github.com/axelberndt/Neues-Thueringer-Choralbuch-digital). All classes of analysis items (such as `Note`, `Chord` and `PitchInterval`) implement their own content-based hash code routine, so they are compatible to `HashMap`, `TreeMap` etc. and to sequence analyses using the provided Markov model implementation.

This library also contains package `performer` which is used to generate expressive performances of chorales, as demonstrated by the [ChoraleWind](https://github.com/groupmm/choralewind) dataset.

As of now, this is purely a programming library, though class `Main` also implements a basic command line interface. This library is rather intended to be used as back-end functionality.

### License information

This library relies on the [meico](https://github.com/cemfi/meico) framework, thus inherits its GNU GPL version 3.0.