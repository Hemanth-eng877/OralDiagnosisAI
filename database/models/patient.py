class Patient:
    def __init__(self, id, user_id, full_name, age=None, gender=None, phone=None, email=None, notes=None):
        self.id = id
        self.user_id = user_id
        self.full_name = full_name
        self.age = age
        self.gender = gender
        self.phone = phone
        self.email = email
        self.notes = notes
