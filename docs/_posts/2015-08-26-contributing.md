---
layout: page
title: "Contributing"
category: doc
date: 2015-08-26 10:41:08
order: 6
---

This project is developed entirely in the open, on public mailing lists and with public code reviews. To participate in development discussions, please subscribe to the [automotive-eg-rvi](https://lists.linuxfoundation.org/mailman/listinfo/automotive-eg-rvi) mailing list, or join the #automotive channel on Freenode. Code is reviewed on [gerrit](https://gerrithub.io). Development is planned and issues are tracked in [JIRA](https://www.atlassian.com/software/jira).

All code contributed to this project must be licensed under the [MPL v2 license](https://www.mozilla.org/MPL/2.0/), a copy of which you can find in this repository. Documentation must be licensed under the [CC BY 4.0 license](https://creativecommons.org/licenses/by/4.0/).

### Development Process

This project is developed with a special focus on secure engineering. In the *docs* folder you will find details of the security architecture and threat model.

During development, any interaction between components must be documented and included in the security modelling. To this end, each project includes a list of implemented requirements and permitted interactions.

Developers must only implement functionality for which there is an associated requirement, described in the project JIRA. When implementing functionality, developers must update the list of [implemented requirements](../ref/requirements.html). Developers must only implement interactions that are permitted or whitelisted according to the associated JIRA ticket. The list of [Whitelisted Interactions](../sec/whitelisted-interactions.html) should be updated when new functionality is implemented, and reviewers should ensure that the code only implements permitted interactions.
