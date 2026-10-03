"""Scope the unchanged reference styles without leaking into the legacy UI."""
import re


def selectors(value):
    result, start, depth = [], 0, 0
    for index, char in enumerate(value):
        if char in '([':
            depth += 1
        elif char in ')]':
            depth -= 1
        elif char == ',' and depth == 0:
            result.append(value[start:index].strip())
            start = index + 1
    return result + [value[start:].strip()]


def scope(css, root):
    css = re.sub(r'/\*.*?\*/', '', css, flags=re.S)
    output, pos = [], 0
    while pos < len(css):
        opening = css.find('{', pos)
        if opening < 0:
            break
        header = css[pos:opening].strip()
        index, depth, quote = opening + 1, 1, ''
        while index < len(css) and depth:
            char = css[index]
            if quote:
                if char == '\\':
                    index += 1
                elif char == quote:
                    quote = ''
            elif char in '\"\'':
                quote = char
            elif char == '{':
                depth += 1
            elif char == '}':
                depth -= 1
            index += 1
        body = css[opening + 1:index - 1]
        if header.startswith(('@media', '@supports', '@layer')):
            body = scope(body, root)
        elif not header.startswith('@'):
            scoped = []
            for selector in selectors(header):
                if selector.startswith(':root'):
                    selector = root + selector[5:]
                elif re.match(r'html\b', selector):
                    selector = root + selector[4:]
                else:
                    selector = root + ' ' + selector
                scoped.append(selector)
            header = ','.join(scoped)
        output.append(header + '{' + body + '}')
        pos = index
    return '\n'.join(output)
