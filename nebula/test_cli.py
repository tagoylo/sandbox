#install nebula first
from fabric import *

c = Connection("host")
o = c.local("cd")
print(o)
o = c.local("nebula --help")
print(o)