# Test script. Tests the 'setMuteLabel()' API call.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

def boolean_prompt(prompt):
    """
    Prompts the user for 'Y' or 'N' and returns True for 'Y' and False for 'N'.

    Args:
        prompt (str): The message to display to the user.

    Returns:
        bool: True if 'Y' is entered, False if 'N' is entered.
              Returns None if an invalid input is given after a retry.
    """
    while True:
        user_input = input(prompt).strip().upper()
        if user_input == 'Y':
            return True
        elif user_input == 'N':
            return False
        else:
            print("Invalid input. Please enter 'Y' for Yes or 'N' for No.")

name = input("Name of the object: ")
mute = boolean_prompt("Mute [Y/N]: ")

gs.setMuteLabel(name, mute)

gateway.shutdown()
