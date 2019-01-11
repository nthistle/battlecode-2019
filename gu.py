a = [1,2,3]

Process.fork do
  a.push(4)
  a.push(5)
  print(a)
end
a.push(4)
