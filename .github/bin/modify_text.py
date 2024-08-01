import re
import sys

## Since workflow files are not true yaml, need to work as a text file of default after finding our keyword
def update_text(file_path, keyword, new_text):
    found_keyword = False
    updated = False

    with open(file_path, 'r') as file:
        lines = file.readlines()

    with open(file_path, 'w') as file:
        for line in lines:
            if not updated and found_keyword and 'default' in line:
                line = re.sub(r"default: '.*'", f"default: '{new_text}'", line)
                updated = True
            if keyword in line:
                found_keyword = True
            file.write(line)

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: modify_text.py <file_path> <keyword> <new_text>")
        sys.exit(1)

    file_path = sys.argv[1]
    keyword = sys.argv[2]
    new_text = sys.argv[3]

    update_text(file_path, keyword, new_text)
