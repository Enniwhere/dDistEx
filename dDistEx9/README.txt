How to demo the program

To demo the program, extract the code and start the editor by typing:
java DistributedTextEditor
from the src folder in the directory the code was unpacked to.
In order to test the Connect, Listen and Disconnect actions, start two different copies of the DistributedTextEditor in from two different terminals. Then select the Listen from the file drop down menu in one of the editors. Once the editor starts listening, enter the IP address and port number from the status line in the listening editor into the corresponding fields in the bottom of the other editor. Then select Connect from the file drop down menu in the second editor to connect the editors. Once connected, whatever is written in the top area of one of the editors will appear in the bottom area of the other editor.
In order to close the connection, select Disconnect from the file menu or simply close one of the editors. The listening editor will continue listening for new connections and the other editor will simply disconnect.
