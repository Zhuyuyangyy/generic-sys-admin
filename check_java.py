import subprocess
import time
import sys

env = {
    'JAVA_HOME': r'D:\Java\jdk-17',
    'PATH': r'D:\Java\jdk-17\bin;' + subprocess.os.environ.get('PATH', ''),
    'DB_PASSWORD': '1234'
}

proc = subprocess.Popen(
    [r'D:\Java\jdk-17\bin\java', '-version'],
    env=env,
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT
)
output, _ = proc.communicate(timeout=5)
print(output.decode('gbk', errors='replace')[:200])
print("Java check done")