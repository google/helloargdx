# Hello AR GDX - Using ARCore with libGDX

This is a port of the hello ar sample for ARCore to libGDX.
The most obvious advantage to this implemention is that there
is no requirement to use or understand OpenGL ES, which is
a large barrier to creating augmented reality apps in Java.

The sample shows the basics of ARCore: plane detection, anchoring objects
in space, and light estimation (coming soon).  In addition, it illustrates
how to use GDX to make an application and integrate non-graphical Android
UI such as Snackbars into the activity.

__DISCLAIMER__
This is not an official Google product.

I work at Google, but this specific project was created personally
as I investigated using ARCore in Java apps.  I can't commit to maintaining
or supporting this project's use.

## Cloning this project
This project contains a git submodule pointing to the ARCore Android SDK.
This submodule needs to present in order to build. To achieve this use the

`--recursive parameter` when cloning:

``
git clone --recursive [URL to Git repo]
``

If you already have cloned the repository and now want to load it’s submodules you have to use submodule update.

``
git submodule update --init
``

# LICENSE
Copyright 2017 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
