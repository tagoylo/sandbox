x = open("input.txt", "r").readlines()
ex = x[0].split(', ')
#print(x)
#print(ex)

def reax(no, p):
    if no <= 11:
        #print("5...........")
        r = 5
    elif 13 <= no <= 24:
        #print("13-------")
        r = 13
    elif no == 12:
        #print("0ooooooooooo")
        r=0

    if p == "y":
        r = r*2
    else:
        r=r 

    return r

F = 0
H = 0
P = 0
for i in ex:
    #print(i)
    cat = i[0]
    t = int(i[1:3])
    photo = i[3]
    #print(cat)
    #print(t)
    #print(photo)

    if cat == "F":
        F = F + reax(t,photo)
        #print("F " +str(F))
    elif cat == "H":
        H = H + reax(t,photo)
        #print("H " + str(H))
    elif cat == "P":
        P = P + reax(t,photo)
        #print("P " +str(P))

prod = F*H*P
print("answer: " +str(prod))
