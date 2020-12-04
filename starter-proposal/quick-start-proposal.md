# Helidon Starter Proposal

## Background

This project started from the idea of adding "features" (e.g. tracing or Open API) to the CLI in the form of options to the `init`
command as well as `add-feature` and `remove-feature` commands. 

A feature is straightforward from a user perspective: a meaningful name that can be selected along with a description
of what it is and when/why it should be used. 

At the implementation level, however, things are much more complex. Ultimately, a feature must be represented in an internal 
data model as some metadata plus one or more of the following:

1. Build file fragments, e.g. pom.xml dependencies 
1. Configuration file fragments
1. README file fragments
1. Java source file fragments, e.g. Main.java
1. Complete source files 

Relationships _across_ features must also be modelled, e.g.
 
1. `A allows B..M`
1. `D requires E`
1. `C and F implies G`
1. `X precedes Y`

In addition to producing and maintaining this data, the use of it implies **_merging_**. At project creation, a base archetype 
must be merged with feature content, and the result must be both functional and properly formatted. A previously created project 
must be merged to add or remove features. 

These are significant requirements when the content is entirely under our control; however, when a user has modified a project 
and wants to add or remove features it becomes a science project. We abandoned the first attempt at supporting features because 
of the latter realization; solving it moving forward requires some simplifying assumptions.  

Further, the scope of this project has been expanded in light of the recognition that we must focus on providing an excellent
"quick start" experience for new users. The important additions are:

1. A Web UI, both to streamline the process (avoids download/install CLI requirement) and to leverage the richer format.
1. A REST service, both for the Web UI and to enable third party (e.g. IDEs) UIs.   

## Non Goals

Given the high likelihood of introducing errors, direct modification of an existing project will not be supported. As an
admittedly poor substitute, we can follow the Micronaut `feature diff` model and rely on knowledgeable users to apply and merge.
(Note that in the CLI we _may_ be able to do better if the project is a git repo, since we can run `git apply --check` to see if
the merge would succeed and then apply it if so, all under some new flag.)

## Goals

1. Ensure that a potential user can search the web and get started with a couple of clicks.
1. Provide a quick start experience on helidon.io that is _guided_ and constrained, exposing complexity only if needed.
1. Expose the data model via a service so that IDE vendors can add project creation support.
1. Ensure that the CLI `init` command matches the web quick-start as much as is reasonable.

## Stretch Goals
 
1. Provide a quick start experience tailored for users with different backgrounds, e.g. Spring Boot, Java EE, Helidon expert, etc.


## High Level User Stories

1. As a developer who has heard of Helidon, I want to easily find a Quick Start page on the web so I can try it out without reading a lot of docs.
1. As a developer who has found the Quick Start page, I don’t want to be told I need anything more than a JDK to try it since I’m not sure I want to use Helidon. 
1. As a first time Quick Start user, I need clear, helpful guidance through the choices so I’m not overwhelmed by things I don’t understand yet.
1. As a Quick Start user, I want to be able to easily navigate back to earlier choices so I can change my mind.
1. As a Quick Start user, I need to be confident that selecting among all of the choices presented to me will result in a working project.
1. As a Quick Start user, I want very simple instructions on how to build, run and use the created project so I don’t have to learn anything else.
1. As a Spring Boot developer, I want Quick Start to help me try out comparable features so I can understand how to migrate and get started.
1. As a Java EE developer, I want Quick Start to help me try out comparable features so I can understand how to migrate and get started.
1. As an experienced Quick Start user, I want to turn off the extra help information so I can create a new project with the features I want very quickly.
1. As a Quick Start user, I want the created project README to contain links for further reading when I’m ready so I don’t have to hunt it down myself.
1. As a Quick Start user, I want to see the rendered README once the project is generated, so I don’t have to read markup.
1. As a developer who has never used Maven, I want the created project README to have some basic help on getting started using it so I don’t have to learn that too.
1. As a new Helidon user, I want the created project README to provide information on installing and using the CLI.
1. As a Helidon CLI user, I want the init command experience to be as close to the Quick Start web experience as possible so it is familiar.
1. As an IDE developer, I want an easily understood REST service for creating new Helidon projects so that I can provide a great user experience.

## Detailed User Stories

[Laird's doc](https://docs.google.com/document/d/1G4IEycA9PDkodNI1d3-Zuj5WoA4oiIn2vKqwt7XAvIo/edit#heading=h.ui9koosmi9gl)


## General Requirements

TODO

## Deliverables

#### Archetype Data Model Updates

TODO

#### Archetype Engine Updates

TODO
 
#### Project Generator Service

TODO

#### Web UI

TODO
