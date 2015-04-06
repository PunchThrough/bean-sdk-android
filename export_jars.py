#!/usr/bin/python

import os
import shutil
from glob import glob
from subprocess import call, check_output

OUTPUT_DIR_NAME = 'jars'


def call_unsafe(*args, **kwargs):
    kwargs['shell'] = True
    call(*args, **kwargs)


call_unsafe('./gradlew clean javadocRelease jarRelease')

try:
    os.mkdir(OUTPUT_DIR_NAME)
except OSError:
    pass

os.chdir(OUTPUT_DIR_NAME)
call_unsafe('rm *.jar')
call_unsafe('cp ../sdk/build/libs/*.jar .')

commit = check_output(['git', 'rev-parse', 'HEAD'])[:7]

for src in glob('*.jar'):
    name, ext = os.path.splitext(src)
    dest = name + '-' + commit + ext
    shutil.move(src, dest)

call_unsafe('open .')
