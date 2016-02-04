#!/usr/bin/python

import sys
import shutil
from glob import glob
from subprocess import call, check_output


if len(sys.argv) < 2:
    print('Please provide an absolute path to a projects libs/ folder')
    sys.exit(1)


target_project_libs_folder = sys.argv[1]


def call_unsafe(*args, **kwargs):
    kwargs['shell'] = True
    call(*args, **kwargs)


call_unsafe('./gradlew clean jarRelease')
for src in glob('./sdk/build/libs/*.jar'):
    shutil.move(src, target_project_libs_folder)
