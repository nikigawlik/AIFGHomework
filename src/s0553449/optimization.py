import re

print("hello world my dude")

f_orig = open("TemplateCar.java", "r")
text_orig = f_orig.read()
f_orig.close()

for i in range(1, 10):
    name = "Car" + str(i)
    f_new = open(name + ".java", "w")

    text = text_orig.replace("/*<n*/TemplateCar/*>*/", "/*<n*/" + name + "/*>*/")
    text = text.replace(' /*<n*/"TemplateCar"/*>*/',
        '/*<n*/"' + name + '"/*>*/')

    f_new.write(text)
    f_new.close()
