#!/usr/bin/env python3
"""Fix FileController.java - writes to file to avoid encoding issues."""

path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\controller\FileController.java'
outpath = r'D:\ZYY Project\generic-sys-admin\fc_fix_log.txt'

with open(path, 'r', encoding='utf-8', errors='replace') as f:
    lines = f.readlines()

log = []
log.append(f'Total lines: {len(lines)}\n')

# Show L285 and L291 (0-indexed: 284, 290)
log.append(f'L285 (idx 284) first 80 chars:\n')
log.append(repr(lines[284][:80]) + '\n')
log.append(f'L291 (idx 290) first 80 chars:\n')
log.append(repr(lines[290][:80]) + '\n')

# Fix line 285 (@Operation summary)
# Correct: summary = "批量删除文件", description = "批量删除多个文件信息，传入文件URL列表"
lines[284] = '    @Operation(summary = "批量删除文件", description = "批量删除多个文件信息，传入文件URL列表")\n'

# Fix line 291 (return error - semicolon at end)
# Correct: return error("文件URL列表为空或格式错误");
lines[290] = '            return error("文件URL列表为空或格式错误");\n'

log.append(f'\nAfter fix:\n')
log.append(f'L285: {repr(lines[284])}\n')
log.append(f'L291: {repr(lines[290])}\n')

# Write back
with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)

log.append('\nFile saved successfully!')

with open(outpath, 'w', encoding='utf-8') as f:
    f.writelines(log)

print('Done - see fc_fix_log.txt')
