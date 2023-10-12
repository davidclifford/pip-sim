import sys
import re
import os

output = [0] * 0x8000
min_addr = 0
max_addr = 0


def usage():
	print("Usage pasm <source.s>")
	exit(0)


def to_num_or_die(token):
	print(token)
	if token[0].isdigit():
		try:
			return int(token)
		except ValueError:
			pass
	if token.startswith('$'):
		try:
			return int(token[1:], 16)
		except ValueError:
			f'Unknown value {token}'
	if token.startswith('%'):
		try:
			return int(token[1:], 2)
		except ValueError:
			f'Unknown value {token}'
	print(f'Expected number no {token}')
	exit(0)


def find_mnemonic(opcode):
	biggest = 0
	index = None
	for m in mne:
		mn = m.replace('_', ' ').replace(',', ' ')
		if opcode[:len(mn)] == mn:
			if len(mn) > biggest:
				biggest = len(mn)
				index = mne.index(m)
			print(f'Found {mn}, index {index} for {opcode}')
	if index is None:
		print(f'Opcode {opcode} not found')
		exit(0)
	return int(hexcode[index]), opcode[biggest+1:], int(oplen[index])


try:
	file = open('opcodes')
	opcodes = file.readlines()
	file.close()
except (FileNotFoundError, IndexError):
	exit("opcodes not found")

mne = []
oplen = []
hexcode = []
for op in opcodes:
	o = op.split(' ')
	hexcode.append(int(o[0], 16))
	oplen.append(int(o[1]))
	mne.append(o[2])

try:
	filename = sys.argv[1]
	file = open(filename)
	lines = file.readlines()
	file.close()
except FileNotFoundError:
	exit(f"Source file {sys.argv[1]} not found")
except IndexError:
	usage()

PC = 0
label = []
label_address = []

# FIRST PASS
for line in lines:
	ln = line.lstrip().rstrip().replace(',', ' ')
	# Comments
	# Ignore blank lines
	if len(ln) == 0:
		continue
	if ln.startswith('#'):
		continue
	first_token = ln.split(' ')[0].lower()
	try:
		second_token = ln.split(' ')[1].lower()
	except IndexError:
		second_token = None
	# ORG
	if first_token == 'org':
		PC = to_num_or_die(second_token)
		continue
	# LABELS
	if first_token.endswith(":"):
		lab = ln.split(':')[0]
		# LABEL
		if lab in label:
			print(f'ERROR: Label "{lab}" already exists')
			exit(1)
		label.append(lab)
		label_address.append(PC)
		print(f'LABEL "{lab}" found, ADDRESS {hex(PC)}')
		ln = ln[len(lab)+1:].lstrip().rstrip()

	if len(ln) == 0:
		continue
	op_code, rest, num_args = find_mnemonic(ln)
	print(f'{hex(PC)} [{ln}]')
	PC += num_args

# SECOND PASS
PC = 0
for line in lines:
	min_addr = min(min_addr, PC)
	max_addr = max(max_addr, PC)
	ln = line.lstrip().rstrip().replace(',', ' ')
	# Comments
	# Ignore blank lines
	if len(ln) == 0:
		continue
	if ln.startswith('#'):
		continue
	# LABELS
	first_token = ln.split(' ')[0].lower()
	if first_token.endswith(":"):
		lab = ln.split(':')[0]
		ln = ln[len(lab)+1:].lstrip().rstrip()
	first_token = ln.split(' ')[0].lower()
	try:
		second_token = ln.split(' ')[1].lower()
	except IndexError:
		second_token = None

	# ORG
	if first_token == 'org':
		PC = to_num_or_die(second_token)
		continue

	if len(ln) == 0:
		continue

	# CODE !!!
	op_code, rest, num_args = find_mnemonic(ln)
	if op_code is not None:
		output[PC] = op_code
		print(f'{hex(PC)} {op_code} [{rest}]')
		PC += 1
	if num_args > 1:
		if rest in label:
			indx = label.index(rest)
			address = label_address[indx]
			print(f'Label {rest} = {address}')
			print(f'{hex(PC)} {op_code} [{rest}]')
			output[PC] = address & 0xff
		elif rest.startswith("'") or rest.startswith('"'):
			output[PC] = ord(rest[1])
		else:
			output[PC] = int(to_num_or_die(rest)) & 0xff
		PC += 1
	max_addr = max(max_addr, PC)

print(f'Min {min_addr} Max {max_addr}')

file_and_ext = os.path.splitext(os.path.basename(filename))
file_path = os.path.dirname(filename)

print(f'C{min_addr:04x}')
bin_out = bytearray()
for i in range(min_addr, max_addr+1):
	print(f'{output[i]:02x}', end=' ')
	bin_out.append(output[i])
print()

# binary_filename = f'{file_path}/{file_and_ext[0]}.bin'
binary_filename = 'instr.bin'
bin_file = open(binary_filename, 'wb')
bin_file.write(bin_out)
bin_file.close()
print(f'Binary file writen to {binary_filename}')
print(bin_out)

