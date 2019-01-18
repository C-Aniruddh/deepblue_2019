from flask import Flask, render_template, url_for, request, session, redirect, send_from_directory, jsonify
import os
from flask_pymongo import PyMongo
import bcrypt
import datetime
import json
import timeago
from geopy.geocoders import Nominatim
from bson.json_util import dumps

from processor import Processor
from werkzeug.utils import secure_filename
import shutil

app = Flask(__name__)
APP_ROOT = os.path.dirname(os.path.abspath(__file__))
DOWNLOAD_FOLDER = os.path.join(APP_ROOT, 'static/downloads')
UPLOAD_FOLDER = os.path.join(APP_ROOT, 'static/uploads')
PROCESSED_FOLDER = os.path.join(APP_ROOT, 'static/processed')

app.config['MONGO_DBNAME'] = 'csp'
app.config['MONGO_URI'] = 'mongodb://localhost:27017/csp'
app.config['DOWNLOAD_FOLDER'] = DOWNLOAD_FOLDER
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['PROCESSED_FOLDER'] = PROCESSED_FOLDER

app.secret_key = 'mysecret'

ALLOWED_EXTENSIONS = {'jpg', 'png', 'jpeg', 'zip'}

mongo = PyMongo(app)

"""
    Snippets : url_for('static', filename='path/to/file')
"""

# Main App

@app.route('/')
def index():
    '''
        Index
    '''
    if 'username' in session:
        return render_template('app/pages/index.html')
    else:
        return redirect('/userlogin')


@app.route('/app/issues/create', methods=['POST', 'GET'])
def create_issue():
    if 'username' in session:

        issues = mongo.db.issues
        if request.method == 'POST':
            image_one = request.files['image_one']
            image_two = request.files['image_two']
            if (image_one and allowed_file(image_two.filename) and (image_two and allowed_file(image_two.filename))):
                image_one_filename = secure_filename(image_one.filename)
                image_two_filename = secure_filename(image_two.filename)
                
                image_one.save(os.path.join(app.config['UPLOAD_FOLDER'], image_one_filename))
                image_two.save(os.path.join(app.config['UPLOAD_FOLDER'], image_two_filename))

                full_path_one = os.path.join(app.config['UPLOAD_FOLDER'], image_one_filename)
                full_path_two = os.path.join(app.config['UPLOAD_FOLDER'], image_two_filename)

                category = request.form.get('project_category')
                description = request.form.get('description')


                current_issues = issues.find()
                total_issue_count = current_issues.count()

                issue_id = str(total_issue_count + 1)

                date = str(datetime.date.today())
                timestamp = str(datetime.datetime.now().strftime('%H:%M:%S'))
                stamp = '%s-%s' % (date, timestamp)

                proc = Processor()
                points, cam1, cam2 = proc.process_files(full_path_one, full_path_two, issue_id)

                lat, lon = proc.get_location_data(full_path_one)

                det, label = proc.get_damage_type(full_path_one, issue_id)

                geolocator = Nominatim(timeout=10)
                loc_string = "%s %s" % (lat, lon)
                location = geolocator.reverse(loc_string)
                
                locality = location.address
                add_s = locality.split(',')
                title_address = ' '.join(add_s[0:3])

                title = "%s at %s" % (label, title_address)

                points_curr = os.path.join(APP_ROOT, points)
                points_dest = os.path.join(app.config['PROCESSED_FOLDER'], points)

                cam1_curr = os.path.join(APP_ROOT, cam1)
                cam1_dest = os.path.join(app.config['PROCESSED_FOLDER'], cam1)

                cam2_curr = os.path.join(APP_ROOT, cam2)
                cam2_dest = os.path.join(app.config['PROCESSED_FOLDER'], cam2)

                det_curr = os.path.join(APP_ROOT, det)
                det_dest = os.path.join(app.config['UPLOAD_FOLDER'], det)

                # Move files to proper directories

                shutil.move(points_curr, points_dest)
                shutil.move(cam1_curr, cam1_dest)
                shutil.move(cam2_curr, cam2_dest)
                shutil.move(det_curr, det_dest)

                issues.insert({'issue_id': issue_id, 'category' : category, 'points' : points_dest, 'cam1' : cam1_dest, 
                    'cam2' : cam2_dest, 'locality': locality, 'image_one_full' : full_path_one, 'image_two_full' : full_path_two, 
                    'description' : description, 'uploaded_by': session['username'], 'time' : stamp, 'detection' : det_dest, 'lat' : lat, 'lon' : lon,
                    'label' : label, 'title' : title})
                
                return redirect('/app/issues/create')

        return render_template('app/pages/create_issue_standalone.html')
    else:
        return redirect('/userlogin')


@app.route('/app/issues')
def existing_issues():
    if 'username' in session:
        issues = mongo.db.issues
        now = datetime.datetime.now() + datetime.timedelta(seconds = 60 * 3.4)

        all_issues = issues.find({})
        
        all_issues_id = []
        all_issues_name = []
        all_issues_age = []
        all_issues_category = []
        all_issues_uploader = []
        all_issues_thumbnail = []
        

        issuelist = range(0, all_issues.count(), 1)

        if all_issues.count() > 0:
            for issue in all_issues:
                all_issues_id.append(issue['issue_id'])
                all_issues_name.append(issue['title'])
                all_issues_age.append(str(timeago.format(issue['time'], now)))
                all_issues_category.append(issue['category'])
                all_issues_uploader.append(issue['uploaded_by'])
                all_issues_thumbnail.append(str("/downloads/%s" % issue['image_one_full'].split('/')[-1]))
        

        return render_template('app/issues/issues.html', issuelist=issuelist, all_issues_id=all_issues_id, all_issues_age=all_issues_age,
            all_issues_name=all_issues_name, all_issues_category=all_issues_category, all_issues_uploader=all_issues_uploader, 
            all_issues_thumbnail=all_issues_thumbnail)
            
    else:
        return redirect('/userlogin')


@app.route('/app/issues/map/view')
def map_view():
    if 'username' in session:
        issues = mongo.db.issues
        now = datetime.datetime.now() + datetime.timedelta(seconds = 60 * 3.4)

        all_issues = issues.find({})
        
        all_issues_id = []
        all_issues_name = []
        all_issues_age = []
        all_issues_category = []
        all_issues_uploader = []
        all_issues_thumbnail = []
        all_issues_lat = []
        all_issues_long = []
        

        issuelist = range(0, all_issues.count(), 1)

        if all_issues.count() > 0:
            for issue in all_issues:
                all_issues_id.append(issue['issue_id'])
                all_issues_name.append(issue['title'])
                all_issues_age.append(str(timeago.format(issue['time'], now)))
                all_issues_category.append(issue['category'])
                all_issues_uploader.append(issue['uploaded_by'])
                all_issues_lat.append(issue['lat'])
                all_issues_long.append(issue['lon'])
                all_issues_thumbnail.append(str("/downloads/%s" % issue['image_one_full'].split('/')[-1]))
        

        return render_template('app/location/location.html', issuelist=issuelist, all_issues_id=all_issues_id, all_issues_age=all_issues_age,
            all_issues_name=all_issues_name, all_issues_category=all_issues_category, all_issues_uploader=all_issues_uploader, 
            all_issues_thumbnail=all_issues_thumbnail, all_issues_lat=all_issues_lat, all_issues_long=all_issues_long)

    return redirect('/userlogin')

@app.route('/app/issues/view/<issue_id>')
def view_issue(issue_id):
    if 'username' in session:
        issues = mongo.db.issues
        issue = issues.find_one({'issue_id' : issue_id})
        issue_category = issue['category']
        issue_description = issue['description']
        issue_uploader = issue['uploaded_by']
        issue_time = issue['time']
        issue_image_one_full = issue['image_one_full']
        issue_image_two_full = issue['image_two_full']
        issue_points_full = issue['points']
        issue_cam1_full = issue['cam1']
        issue_cam2_full = issue['cam2']
        issue_det_full = issue['detection']

        issue_lat = str(issue['lat'])
        issue_lon = str(issue['lon'])
        issue_title = issue['title']
        issue_location = issue['locality']

        issue_points = "/cdn/pointcloud/%s" % issue_points_full.split('/')[-1]
        issue_cam1 = "/cdn/pointcloud/%s" % issue_cam1_full.split('/')[-1]
        issue_cam2 = "/cdn/pointcloud/%s" % issue_cam2_full.split('/')[-1]

        issue_image_one = "/downloads/%s" % issue_image_one_full.split('/')[-1]
        issue_image_two = "/downloads/%s" % issue_image_two_full.split('/')[-1]

        issue_image_det = "/downloads/%s" % issue_det_full.split('/')[-1]

        return render_template('app/pages/single_issue.html', issue_id=issue_id, issue_category=issue_category, issue_description=issue_description,
            issue_uploader=issue_uploader, issue_time=issue_time, issue_points=issue_points, issue_cam1=issue_cam1, issue_cam2=issue_cam2,
            issue_image_one=issue_image_one, issue_image_two=issue_image_two, issue_lat=issue_lat, issue_lon=issue_lon, issue_title=issue_title,
            issue_image_det=issue_image_det, issue_location=issue_location)
    else:
        return redirect('/userlogin')


@app.route('/app/submit')
def app_submit():
    return redirect('/app/issues/create')


# Mobile API

@app.route('/mobile/issues')
def mobile_issues():
    issues = mongo.db.issues
    all_issues = issues.find({}, {'_id': False})
    return dumps(all_issues)

# User management
# Login and register 
@app.route('/register', methods=['POST', 'GET'])
def register():
    if 'username' in session:
        return redirect('/')
    if request.method == 'POST':
        users = mongo.db.users
        user_fname = request.form.get('name')
        # user_fname = request.form['name']
        user_email = request.form.get('email')
        existing_user = users.find_one({'name': request.form.get('username')})
        if existing_user is None:
            hashpass = bcrypt.hashpw(request.form.get('password').encode('utf-8'), bcrypt.gensalt())
            users.insert(
                {'fullname': user_fname, 'email': user_email, 'name': request.form.get('username'),
                 'user_type': 'worker', 'password': hashpass})
            session['username'] = request.form.get('username')
            return redirect('/')

        return 'A user with that Email id/username already exists'

    return render_template('app/pages/signup.html')

@app.route('/login', methods=['POST'])
def login():
    users = mongo.db.users
    login_user = users.find_one({'name': request.form['username']})

    if login_user:
        if bcrypt.hashpw(request.form.get('password').encode('utf-8'), login_user['password']) == login_user[
            'password']:
            session['username'] = request.form['username']
            return redirect('/')

    return 'Invalid username/password combination'

@app.route('/userlogin', methods=['POST', 'GET'])
def userlogin():
    if 'username' in session:
        return redirect('/')

    return render_template('app/pages/login.html')

@app.route('/logout')
def logout():
    session.pop('username', None)
    return redirect('/')


# Download management

@app.route('/downloads/<filename>')
def downloads(filename):
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)


@app.route('/cdn/pointcloud/<filename>')
def pointcloud_cdn(filename):
    return send_from_directory(app.config['PROCESSED_FOLDER'], filename)


# Page Errors

@app.errorhandler(404)
def page_not_found(e):
    return render_template('app/pages/error-404.html'), 404


@app.errorhandler(500)
def page_unresponsive(e):
    return render_template('app/pages/error-500.html'), 500

# Extra functions
def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1] in ALLOWED_EXTENSIONS

if __name__ == "__main__":
    app.run(host='0.0.0.0')
