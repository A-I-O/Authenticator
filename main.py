from flask import Flask, request, jsonify
from bs4 import BeautifulSoup
import requests
import os
import psycopg2
from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas

app = Flask(__name__)

def connect_to_database():
    # Connect to the PostgreSQL database
    conn = psycopg2.connect(
        dbname="authenticator",
        user="postgres",
        password="Ivan",
        host="localhost",
        port="5432"
    )
    return conn

def insert_profile_data_to_db(profile_data):
    conn = connect_to_database()
    cur = conn.cursor()
    try:
        # Insert profile data into the response table
        cur.execute(
            "INSERT INTO response (name, registration_number, program, profile_picture_path, fee_balance) VALUES (%s, %s, %s, %s, %s)",
            (profile_data['name'], profile_data['registration_number'], profile_data['program'], profile_data['profile_picture_path'], profile_data['fee_balance'])
        )
        conn.commit()
    except Exception as e:
        conn.rollback()
        print("Error inserting data into the database:", e)
    finally:
        cur.close()
        conn.close()

def scrape_fee_balance(session):
    financials_url = 'https://portal.mmust.ac.ke/financials/Financials'
    financials_response = session.get(financials_url)
    financials_soup = BeautifulSoup(financials_response.content, 'html.parser')
    fee_balance_element = financials_soup.find('span', {'class': 'fw-semibold'})
    if fee_balance_element:
        fee_balance = fee_balance_element.text.strip()
    else:
        fee_balance = 'Not available'
    return fee_balance

def scrape_profile(session):
    profile_url = 'https://portal.mmust.ac.ke/Home/Profile'
    profile_response = session.get(profile_url)
    soup = BeautifulSoup(profile_response.content, 'html.parser')
    registration_number = soup.find('input', {'id': 'StudentProfileData_Register_AdmnNo'}).get('value')
    name = soup.find('input', {'id': 'StudentProfileData_Register_Names'}).get('value')
    program = soup.find('input', {'id': 'StudentProfileData_Register_Programme'}).get('value')
    profile_picture_src = soup.find('img', {'id': 'profilePicture'}).get('src')
    base_url = 'https://portal.mmust.ac.ke'
    profile_picture_src = profile_picture_src.replace('\\', '/')
    if not profile_picture_src.startswith('http'):
        profile_picture_src = base_url + profile_picture_src

    # Extract file extension from the URL
    file_extension = os.path.splitext(profile_picture_src)[1]
    # Generate filename with the student's name
    profile_picture_filename = f"{name}{file_extension}"
    profile_picture_path = os.path.join(os.path.dirname(__file__), profile_picture_filename)

    # Check if the file with the same name already exists
    existing_files = [f for f in os.listdir(os.path.dirname(__file__)) if f.endswith(file_extension)]
    if any(existing_file.startswith(name) for existing_file in existing_files):
        # If a file with a similar name exists, overwrite it
        with open(profile_picture_path, 'wb') as f:
            profile_picture_response = session.get(profile_picture_src)
            f.write(profile_picture_response.content)
    else:
        # If no file with a similar name exists, store the picture with the student's name as the filename
        with open(profile_picture_path, 'wb') as f:
            profile_picture_response = session.get(profile_picture_src)
            f.write(profile_picture_response.content)

    profile_data = {
        'name': name,
        'registration_number': registration_number,
        'program': program,
        'profile_picture_path': profile_picture_path
    }
    return profile_data

def write_to_pdf(response_data):
    pdf_file_path = os.path.join(os.path.dirname(__file__), 'response_log.pdf')
    c = canvas.Canvas(pdf_file_path, pagesize=letter)
    y = 750
    for key, value in response_data['profile_data'].items():
        if key == 'profile_picture_path':
            c.drawImage(value, 100, y - 20, width=100, height=100)
        else:
            c.drawString(250, y, f"{key}: {value}")
            y -= 20
    c.save()

@app.route('/login', methods=['POST'])
def login():
    username = request.json.get('username')
    password = request.json.get('password')
    print(f"Received login request with username: {username} and password: {password}")
    login_url = 'https://portal.mmust.ac.ke/'
    session = requests.Session()
    login_payload = {
        'username': username,
        'password': password
    }
    print("Attempting to log in to the portal...")
    login_response = session.post(login_url, data=login_payload, verify=False)
    print("Login request sent.")
    if 'logout' in login_response.text:
        print("Login successful.")
        profile_data = scrape_profile(session)
        fee_balance = scrape_fee_balance(session)
        profile_data['fee_balance'] = fee_balance
        response_data = {'status': 'success', 'message': 'Login successful', 'profile_data': profile_data}
        write_to_pdf(response_data)
        insert_profile_data_to_db(profile_data)  # Insert profile data into the database
        print("Response to app:", response_data)
        return jsonify(response_data), 200
    else:
        print("Login failed.")
        return jsonify({'status': 'error', 'message': 'Login failed. Incorrect username or password.'}), 401

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
