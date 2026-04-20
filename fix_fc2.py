#!/usr/bin/env python3
"""Fix FileController.java - multi-line @Operation replacement."""

path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\controller\FileController.java'

with open(path, 'r', encoding='utf-8', errors='replace') as f:
    content = f.read()

lines = content.split('\n')
log = []
log.append(f'Total lines: {len(lines)}\n')

# Show the problematic area
log.append('Current L284-L288:\n')
for i in range(283, 288):
    log.append(f'L{i+1}: {repr(lines[i][:100])}\n')

# The @Operation annotation spans L285 and L286 (0-indexed: 284, 285)
# L284: @DeleteMapping("/batch")
# L285: @Operation(summary = "<corrupted_summary>?)
# L286: description = "<corrupted_desc>?")
# We need to replace both L285 and L286

# Correct replacement for the batch delete @Operation
new_l285 = '    @Operation(summary = "批量删除文件", description = "批量删除多个文件信息，传入文件URL列表")'
new_l286 = ''  # empty line or we can keep it empty since the annotation is now on one line

log.append(f'\nNew L285: {repr(new_l285)}\n')
log.append(f'New L286: {repr(new_l286)}\n')

# Replace
lines[284] = new_l285
# Don't delete L286, just make it empty or keep as the start of description
# Actually looking at the original: L286 starts with "                .description("
# Let me check - does L286 start with spaces + ".description"?
log.append(f'\nL286 full: {repr(lines[285])}\n')

# The description part starts with "                .description("
# We need to incorporate the description into L285, so L286 should be empty
lines[285] = ''

log.append(f'\nAfter replacement:\n')
log.append(f'L285: {repr(lines[284])}\n')
log.append(f'L286: {repr(lines[285])}\n')
log.append(f'L287: {repr(lines[286])}\n')

# Also fix L291 (idx 290): the return error statement
log.append(f'\nL291 (idx 290) before fix: {repr(lines[290][:80])}\n')
lines[290] = '            return error("文件URL列表为空或格式错误");'
log.append(f'L291 after fix: {repr(lines[290])}\n')

# Write back
with open(path, 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines))

log.append('\nFile saved!')

with open(r'D:\ZYY Project\generic-sys-admin\fc_fix2_log.txt', 'w', encoding='utf-8') as out:
    out.writelines(log)

print('Done')
