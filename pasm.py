import sys
import re
import os

output = [0] * 0x10000
min_addr = 0
max_addr = 0
jsr = 0xfe00 - 2
rts = {}


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
	return int(hexcode[index]), opcode[biggest+1:].lstrip().rstrip(), int(oplen[index])


def get_mnemonic(opcode):
	for m in mne:
		index = mne.index(m)
		if hexcode[index] == opcode:
			return mne[index], oplen[index]
	print(f'Cant find {opcode:02x}')
	exit(1)


def get_value(rest):
	if rest[0] == '>':
		rest = rest[1:]
		if rest in label:
			indx = label.index(rest)
			address = label_address[indx]
			print(f'Label {rest} = {address}')
			print(f'{hex(PC)} {op_code} [{rest}]')
			return address >> 8 & 0xff
	elif rest in label:
		indx = label.index(rest)
		address = label_address[indx]
		print(f'Label {rest} = {address}')
		print(f'{hex(PC)} {op_code} [{rest}]')
		return address & 0xff
	elif rest.startswith("'") or rest.startswith('"'):
		return ord(rest[1])
	else:
		return int(to_num_or_die(rest)) & 0xff


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
print('--- FIRST PASS ---')
for line in lines:
	ln = line.lstrip().rstrip().replace(',', ' ')
	# Comments
	# Ignore blank lines
	if len(ln) == 0:
		continue
	if ln.startswith('#'):
		continue

	# LABELS
	if ln.split(' ')[0].endswith(":"):
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

	first_token = ln.split(' ')[0].lower()
	try:
		second_token = ln.split(' ')[1]
	except IndexError:
		second_token = None

	print(f'[{first_token}]')

	# ORG
	if first_token == 'org':
		PC = to_num_or_die(second_token)
		continue
	# DB
	if first_token == 'db':
		if second_token is None:
			PC += 1
		elif second_token.startswith("'"):  # Non-zero terminarted strings
			PC += (len(second_token) - 2)
		elif second_token.startswith('"'):  # Zero terminated strings
			PC += (len(second_token) - 1)
		elif second_token.startswith('$'):  # Hex numbers
			PC += 1
		elif second_token.startswith('%'):  # Binary numbers
			PC += 1
		elif second_token[0].isdigit():  # Numbers
			PC += 1
		elif second_token.startswith('('):  # Binary numbers
			if second_token.endswith(')'):
				PC += to_num_or_die(second_token[1:-1])
			else:
				print('Missing closing )')
				exit(1)
		else:
			print(f'Unknown param {second_token}')
		continue

	# OPCODES
	op_code, rest, num_args = find_mnemonic(ln)
	print(f'{hex(PC)} [{ln}]')
	PC += num_args

# SECOND PASS
print('--- SECOND PASS ---')
PC = 0
for line in lines:
	min_addr = min(min_addr, PC)
	if PC < 0x8000:
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
		second_token = ln.split(' ')[1]
	except IndexError:
		second_token = None

	if len(ln) == 0:
		continue

	# ORG
	if first_token == 'org':
		PC = to_num_or_die(second_token)
		continue

	# DB Data Bytes, String ' ', StringZ " ", Number 255,$ff,%11111111, Size (4)
	if first_token == 'db':
		second_token = line[line.find('db')+2:].lstrip().rstrip()
		if second_token is None or second_token == '':
			PC += 1
		elif second_token.startswith("'"):  # Non-zero terminated strings
			for c in second_token[1:-1]:
				output[PC] = ord(c)
				PC += 1
		elif second_token.startswith('"'):  # Zero terminated strings
			for c in second_token[1:-1]:
				output[PC] = ord(c)
				PC += 1
			output[PC] = 0
			PC += 1
		elif second_token.startswith('$'):  # Hex numbers
			output[PC] = to_num_or_die(second_token) & 0xff
			PC += 1
		elif second_token.startswith('%'):  # Binary numbers
			output[PC] = to_num_or_die(second_token) & 0xff
			PC += 1
		elif second_token[0].isdigit():  # Numbers
			output[PC] = to_num_or_die(second_token) & 0xff
			PC += 1
		elif second_token.startswith('('):  # Binary numbers
			if second_token.endswith(')'):
				PC += to_num_or_die(second_token[1:-1])
			else:
				print('Missing closing )')
				exit(1)
		else:  # db defaults to 1 byte length
			PC += 1
		continue

	# JSR and RTS macros
	if first_token == 'jsr':
		if jsr >= 0xfefe:
			print('No more return values available')
			exit(1)

		_, rest, _ = find_mnemonic(ln)
		ret_val = get_value(rest)
		if ret_val in rts:
			jsr = rts[ret_val]
		else:
			jsr += 2
			rts[ret_val] = jsr
		return_address = PC + 14

		op_code, _, _ = find_mnemonic('MOV H')
		output[PC] = op_code
		PC += 1
		output[PC] = jsr >> 8
		PC += 1

		op_code, _, _ = find_mnemonic('MOV L')
		output[PC] = op_code
		PC += 1
		output[PC] = jsr & 0xff
		PC += 1

		op_code, _, _ = find_mnemonic('STO')
		output[PC] = op_code
		PC += 1
		output[PC] = return_address & 0xff
		PC += 1

		op_code, _, _ = find_mnemonic('MOV L')
		output[PC] = op_code
		PC += 1
		output[PC] = (jsr+1) & 0xff
		PC += 1

		op_code, _, _ = find_mnemonic('STO')
		output[PC] = op_code
		PC += 1
		output[PC] = return_address >> 8
		PC += 1

		op_code, _, _ = find_mnemonic('MOV T')
		output[PC] = op_code
		PC += 1
		val = get_value(rest)
		output[PC] = val >> 8
		PC += 1

		op_code, _, _ = find_mnemonic('JMP')
		output[PC] = op_code
		PC += 1
		output[PC] = get_value(rest) & 0xff
		PC += 1
		continue

	if first_token == 'rts':
		_, rest, _ = find_mnemonic(ln)
		ret_val = rts[get_value(rest)]

		op_code, _, _ = find_mnemonic('MOV H')
		output[PC] = op_code
		PC += 1
		output[PC] = (ret_val+1) >> 8
		PC += 1

		op_code, _, _ = find_mnemonic('MOV L')
		output[PC] = op_code
		PC += 1
		output[PC] = (ret_val+1) & 0xff
		PC += 1

		op_code, _, _ = find_mnemonic('LDT')
		output[PC] = op_code
		PC += 1

		op_code, _, _ = find_mnemonic('MOV L')
		output[PC] = op_code
		PC += 1
		output[PC] = ret_val & 0xff
		PC += 1

		op_code, _, _ = find_mnemonic('LDP')
		output[PC] = op_code
		PC += 1

		continue

	# CODE !!!
	op_code, rest, num_args = find_mnemonic(ln)
	if op_code is not None:
		output[PC] = op_code
		print(f'{hex(PC)} {op_code} [{rest}]')
		PC += 1
	if num_args > 1:
		output[PC] = get_value(rest)
		PC += 1
	if PC < 0x8000:
		max_addr = max(max_addr, PC)

if PC < 0x8000:
	max_addr = max(max_addr, PC)

print(f'Min {min_addr} Max {max_addr}')

file_and_ext = os.path.splitext(os.path.basename(filename))
file_path = os.path.dirname(filename)

print(f'C{min_addr:04x}')
bin_out = bytearray()
for i in range(min_addr, max_addr):
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

# Disassembler
print()
print('Disassembly')
print()
PC = min_addr
while PC < max_addr:
	addr = PC
	lbl  = ''
	if addr in label_address:
		idx = label_address.index(addr)
		print(f'{label[idx]}:')
	op = output[PC]
	PC += 1
	mnemonic, op_len = get_mnemonic(op)
	if op_len > 1:
		param = output[PC]
		PC += 1
		print(f'{addr:04x} {mnemonic} {param:02x}')
	else:
		print(f'{addr:04x} {mnemonic}')
