import re
import random

print("hello world my dude")

f_orig = open("TemplateCar.java", "r")
text_orig = f_orig.read()
f_orig.close()

NUMBER_OF_CARS = 30

for i in range(1, NUMBER_OF_CARS):
    name = "Car" + str(i)
    f_new = open(name + ".java", "w")

    text = text_orig.replace(
        "/*<n*/TemplateCar/*>*/",
        "/*<n*/" + name + "/*>*/"
    )
    text = text.replace(
        '/*<n*/"TemplateCar"/*>*/',
        '/*<n*/"' + name + '"/*>*/'
    )

    def repl(match):
        num = match.group(2)
        txt = str(float(num) * random.randrange(80, 120) / 100)
        return match.group(1) + txt + match.group(4)

    text = re.sub(r"(/\*<f\*/)(\d+(\.\d+)?)(f/\*>\*/)", repl, text)

    f_new.write(text)
    f_new.close()
