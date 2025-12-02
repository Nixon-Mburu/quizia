-- SQLite schema and seed data for Quizia
PRAGMA foreign_keys = ON;

BEGIN TRANSACTION;

-- users table
CREATE TABLE IF NOT EXISTS users (
  user_id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT NOT NULL UNIQUE
);

-- rooms table (room registry)
CREATE TABLE IF NOT EXISTS rooms (
  room_id TEXT PRIMARY KEY,
  room_name TEXT NOT NULL,
  created_by INTEGER,
  created_at TEXT,
  FOREIGN KEY(created_by) REFERENCES users(user_id)
);

-- registered_rooms table with six columns (room_id, room_name, member_count, member_names, topics, created_by_username, created_at)
CREATE TABLE IF NOT EXISTS registered_rooms (
  room_id TEXT PRIMARY KEY,
  room_name TEXT NOT NULL,
  member_count INTEGER DEFAULT 0,
  member_names TEXT,
  topics TEXT,
  created_by_username TEXT,
  created_at TEXT
);

-- questions table
CREATE TABLE IF NOT EXISTS questions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  topic TEXT NOT NULL,
  question TEXT NOT NULL,
  option_a TEXT,
  option_b TEXT,
  option_c TEXT,
  option_d TEXT,
  correct_option TEXT
);

-- results table: stores per-attempt results for leaderboard
CREATE TABLE IF NOT EXISTS results (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  room_id TEXT NOT NULL,
  username TEXT NOT NULL,
  correct INTEGER NOT NULL,
  total_time_ms INTEGER NOT NULL,
  submitted_at TEXT DEFAULT (datetime('now'))
);

-- answers table: stores per-question answer times for detailed timing analysis
CREATE TABLE IF NOT EXISTS answers (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  room_id TEXT NOT NULL,
  username TEXT NOT NULL,
  question_index INTEGER NOT NULL,
  selected_answer TEXT,
  time_ms INTEGER NOT NULL,
  is_correct INTEGER NOT NULL, -- 1 for correct, 0 for incorrect
  submitted_at TEXT DEFAULT (datetime('now'))
);

-- Create indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_rooms_created_by ON rooms(created_by);
CREATE INDEX IF NOT EXISTS idx_registered_rooms_created_by ON registered_rooms(created_by_username);
CREATE INDEX IF NOT EXISTS idx_results_room_id ON results(room_id);
CREATE INDEX IF NOT EXISTS idx_results_username ON results(username);
CREATE INDEX IF NOT EXISTS idx_results_room_username ON results(room_id, username);
CREATE INDEX IF NOT EXISTS idx_answers_room_id ON answers(room_id);
CREATE INDEX IF NOT EXISTS idx_answers_username ON answers(username);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Seed a few example users
INSERT OR IGNORE INTO users (user_id, username) VALUES (1, 'alice');
INSERT OR IGNORE INTO users (user_id, username) VALUES (2, 'bob');
INSERT OR IGNORE INTO users (user_id, username) VALUES (3, 'charlie');

-- Seed a couple of rooms
INSERT OR IGNORE INTO rooms (room_id, room_name, created_by, created_at) VALUES ('R001','General Trivia','1', datetime('now'));
INSERT OR IGNORE INTO rooms (room_id, room_name, created_by, created_at) VALUES ('R002','Science League','2', datetime('now'));

-- Seed registered_rooms equivalent rows (member_names comma-separated)
INSERT OR IGNORE INTO registered_rooms (room_id, room_name, member_count, member_names, topics, created_by_username, created_at) VALUES ('R001','General Trivia',3,'alice,bob,charlie','General Knowledge', 'alice', datetime('now'));
INSERT OR IGNORE INTO registered_rooms (room_id, room_name, member_count, member_names, topics, created_by_username, created_at) VALUES ('R002','Science League',1,'bob','Science & Technology', 'bob', datetime('now'));

-- Questions seed: 6 topics x 30 = 180 questions
-- Topic: General Knowledge (30)
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','What is the chemical symbol for water?','H2','O2','H2O','HO','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','Which organ pumps blood around the body?','Liver','Heart','Kidney','Lung','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','What gas do plants take in to perform photosynthesis?','Oxygen','Nitrogen','Carbon Dioxide','Hydrogen','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','Which planet is known as the Red Planet?','Venus','Mars','Jupiter','Mercury','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','How many continents are there on Earth?','5','6','7','8','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','What is the largest mammal?','Elephant','Blue Whale','Giraffe','Hippopotamus','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','What color do you get when you mix red and white?','Pink','Purple','Orange','Brown','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','Which device is used to look at very small objects?','Telescope','Microscope','Periscope','Binoculars','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','Which shape has three sides?','Square','Triangle','Circle','Hexagon','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','What is the freezing point of water in Celsius?','0','32','-10','100','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','Which is the fastest land animal?','Cheetah','Lion','Horse','Gazelle','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','What is the capital of the world?','United Nations','There is none','Geneva','New York','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','How many minutes are in an hour?','30','45','60','90','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','Which element has the symbol Au?','Silver','Gold','Iron','Copper','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','What do bees produce?','Milk','Wax','Honey','Silk','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','Which is a primary color?','Green','Purple','Red','Pink','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','What is 7 + 6?','12','13','14','15','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','Which instrument has keys and produces musical notes?','Guitar','Piano','Drum','Violin','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','Which is used to write on paper?','Brush','Pen','Spoon','Hammer','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','What is the boiling point of water at sea level in Celsius?','0','50','100','150','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','Which of these is a fruit?','Carrot','Potato','Apple','Onion','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','What is the opposite of hot?','Warm','Cold','Lukewarm','Boiling','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','How many legs does a spider have?','6','8','10','12','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','Which sense do we use to smell?','Sight','Taste','Smell','Touch','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','Which material is used to make glass?','Sand','Wood','Iron','Plastic','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','What is the main language used on the internet for web pages?','Python','HTML','C++','SQL','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','Which day comes after Monday?','Sunday','Tuesday','Friday','Wednesday','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('General Knowledge','Which body part helps you hear?','Eye','Ear','Nose','Tongue','B');

-- Topic: Science & Technology (30)
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','What is the basic unit of life?','Atom','Molecule','Cell','Organ','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','What does CPU stand for?','Central Processing Unit','Core Process Unit','Central Program Unit','Control Processing Unit','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','Which force keeps us on the ground?','Magnetism','Gravity','Friction','Electricity','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','Light travels fastest in which medium?','Air','Glass','Vacuum','Water','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','What does DNA stand for?','Deoxyribonucleic Acid','Dynamic Nucleic Acid','Deoxy Nuclear Acid','None of these','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','Which planet is largest in our solar system?','Saturn','Jupiter','Earth','Neptune','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','Which gas is most abundant in Earth''s atmosphere?','Oxygen','Nitrogen','Carbon Dioxide','Helium','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','What device converts chemical energy into electrical energy?','Motor','Battery','Generator','Transformer','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','What is the boiling point of water in Celsius?','50','100','150','200','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','Which organelle is the powerhouse of the cell?','Nucleus','Mitochondrion','Ribosome','Golgi apparatus','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','Which scale measures earthquake magnitude?','Richter','Celsius','Kelvin','Beaufort','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','What does HTTP stand for?','HyperText Transfer Protocol','Hyperlink Text Transfer Protocol','High Transfer Text Protocol','None','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','What is robotics mainly concerned with?','Plants','Animals','Machines','Minerals','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','Which field studies the structure of the Earth?','Astronomy','Geology','Biology','Linguistics','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','What material is used to make computer chips?','Plastic','Silicon','Wood','Glass','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','What unit is used to measure electrical resistance?','Volt','Ohm','Ampere','Watt','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','What does AI stand for in technology?','Automatic Interface','Artificial Intelligence','Applied Internet','Active Input','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','Which particle has a negative charge?','Proton','Electron','Neutron','Positron','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','Which instrument measures temperature?','Barometer','Thermometer','Hygrometer','Anemometer','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','Which planet has rings?','Earth','Mars','Saturn','Venus','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','What is the term for frozen water?','Rain','Snow','Ice','Hail','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','What is the study of living organisms called?','Chemistry','Biology','Physics','Geology','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','Electric current is measured in which unit?','Volt','Ohm','Ampere','Watt','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','Which technology is used to broadcast radio signals?','WiFi','Bluetooth','AM/FM','USB','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','Which chemical element is gaseous at room temperature?','Iron','Gold','Oxygen','Silicon','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','Which instrument is used to observe stars?','Microscope','Telescope','Stethoscope','Thermometer','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','What does GPU stand for?','Graphics Processing Unit','General Processing Unit','Graphical Program Unit','Global Processing Unit','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Science & Technology','Which kind of energy comes from the Sun?','Thermal only','Solar','Geothermal','Nuclear','B');

-- Topic: Pop Culture & Entertainment (30)
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which medium is associated with Oscars?','Music','Television','Film','Literature','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which instrument is commonly used in rock bands?','Saxophone','Electric Guitar','Oboe','Cello','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','What do we call short online video clips that loop?','Podcasts','GIFs','Documents','Spreadsheets','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which is a popular streaming service for video?','Spotify','Netflix','SoundCloud','Audible','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which term describes a bestselling book?','Flop','Bestseller','Outdated','Draft','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which art form uses choreography?','Ballet','Poetry','Painting','Sculpture','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','What is cosplay?','Cooking','Dressing as characters','Singing','Photography','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which award recognizes achievements in music?','Grammy','Nobel','Pulitzer','Emmy','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which device plays video games?','Television only','Game console','Microscope','Stove','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which popular video platform hosts user videos?','YouTube','HTTP','FTP','SMTP','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which genre focuses on investigation and criminal cases?','Romance','Mystery','Fantasy','Sci‑Fi','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which format is a serialized television story?','Movie','TV series','Painting','Booklet','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','What is a hit single?','A popular song release','A failed album','A concert ticket','A poster','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which instrument has 88 keys?','Violin','Piano','Flute','Guitar','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','What does CGI stand for?','Computer-Generated Imagery','Creative Graphic Input','Cinema Graphic Interface','Computer Graph Input','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','What is a blockbuster usually referring to?','A small indie film','A commercially successful film','A book','A song','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which is a dance style?','Opera','Salsa','Essay','Sketch','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Who typically directs a film?','Actor','Director','Editor','Composer','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which is an example of social media?','Newspaper','Instagram','Radio','Telephone','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which item is used for stage makeup?','Brush','Pipette','Scissors','Hammer','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which instrument uses a bow?','Guitar','Violin','Piano','Drum','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which medium is primarily audio?','Podcast','Comic book','Film','Painting','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','What is a sequel?','An original story','A follow-up work','A review','An advertisement','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which is a dance musical?','Ballet','Novel','Essay','Documentary','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which item tracks follower counts?','Thermometer','Social media profile','Calendar','Ruler','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','What is fan art?','Official product','Artwork created by fans','Legal contract','Software','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Pop Culture & Entertainment','Which is a musical performance by singers?','Orchestra','Choir','Library','Museum','B');

-- Topic: Geography (30)
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','What is the largest ocean on Earth?','Atlantic','Indian','Arctic','Pacific','D');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','Which is the longest type of landform?','Mountain range','Valley','Plain','Desert','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','What is a peninsula?','Land surrounded by water on three sides','A small island','A mountain peak','A river mouth','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','Which line divides Earth into Northern and Southern Hemispheres?','Prime Meridian','International Date Line','Equator','Tropic of Cancer','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','What is the study of maps called?','Cartography','Glaciology','Meteorology','Oceanography','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','What do we call a large area of low rainfall?','Forest','Desert','Tundra','Marsh','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','Which is a freshwater lake feature?','Ocean','Lake','Volcano','Glacier','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','What is an archipelago?','A group of islands','A type of mountain','A desert type','A large river','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','Which term describes a line of longitude?','Parallel','Meridian','Contour','Zone','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','What is the term for a narrow strip of land connecting two larger land areas?','Isthmus','Peninsula','Archipelago','Delta','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','Which feature is formed by moving ice?','Volcano','Glacier','Dune','Oasis','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','What is a delta?','Mountain peak','River mouth deposit','A valley','A plateau','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','Which is the study of climates?','Climatology','Psychology','Biology','Theology','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','What is a plateau?','Flat elevated land','Low flat land','Steep valley','Coastal area','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','Which is saltwater?','Lake','River','Ocean','Pond','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','What is the process by which water vapor becomes liquid?','Evaporation','Condensation','Sublimation','Transpiration','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','Which feature is a low area between hills?','Plateau','Valley','Desert','Isthmus','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','Which is a tropical ecosystem?','Tundra','Rainforest','Taiga','Steppe','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','What is a coastline?','Boundary between land and sea','Mountain top','Riverbed','Forest edge','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','Which is a human-made water channel?','River','Canal','Glacier','Delta','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','What is an island?','Land surrounded by water','A large continent','A mountain','A forest','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','Which is a major factor in erosion?','Wind','Thought','Silence','Gravity','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','What is permafrost?','Permanently frozen ground','A type of rock','A river feature','A sand dune','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','Which latitude runs through 0 degrees?','Tropic of Cancer','Equator','Prime Meridian','Arctic Circle','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','What is a canyon?','A deep valley typically with a river','A flat plain','A small island','A hill','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','Which term describes land near water?','Inland','Coastal','Mountainous','Polar','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Geography','Which holds more water: ocean or river?','River','Ocean','Both equal','Depends','B');

-- Topic: Sports (30)
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','How many players are on a soccer team on the field per side?','9','10','11','12','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport uses a racket and shuttlecock?','Tennis','Badminton','Table Tennis','Squash','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport is played in a velodrome?','Swimming','Cycling','Basketball','Rugby','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','How many bases are there in baseball?','2','3','4','5','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport uses a puck?','Football','Hockey','Cricket','Tennis','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport is associated with Wimbledon?','Cricket','Tennis','Golf','Rugby','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','What is a touchdown worth in American football?','3','6','1','2','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which equipment is used in golf?','Club','Racket','Bat','Paddle','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport is in the Summer Olympics: rowing or ice hockey?','Rowing','Ice hockey','Both','Neither','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport uses a hoop and backboard?','Basketball','Netball','Hockey','Baseball','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which game uses wickets and overs?','Baseball','Cricket','Tennis','Football','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','How many players in a basketball team on the court per side?','4','5','6','7','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport is performed on ice?','Curling','Basketball','Rugby','Cricket','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport uses a shuttlecock?','Badminton','Tennis','Squash','Table Tennis','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which event measures running speed over short distances?','Marathon','Sprint','Relay','Hurdles','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport has the Tour de France?','Cycling','Sailing','Rowing','Skiing','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport uses a pommel horse?','Gymnastics','Weightlifting','Boxing','Archery','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport requires a ball and a net across a court?','Volleyball','Rugby','Swimming','Golf','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','What color card indicates a serious foul in soccer?','Yellow','Blue','Red','Green','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','In tennis, how many points is a game won by after deuce?','One more point than opponent','Two consecutive points','Three points','No extra points','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which equipment is essential for ice skating?','Skates','Cleats','Flippers','Gloves','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport is performed in a ring with ropes?','Wrestling/Boxing','Basketball','Hockey','Baseball','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport uses sails?','Rowing','Sailing','Cycling','Fencing','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport uses goalposts and a crossbar?','Rugby/Football','Tennis','Volleyball','Basketball','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','What is the playing surface for cricket usually called?','Pitch','Green','Field','Court','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport combines swimming, cycling and running?','Triathlon','Duathlon','Pentathlon','Decathlon','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('Sports','Which sport uses a shuttle and a net?','Badminton','Basketball','Football','Baseball','A');

-- Topic: History & Politics (30)
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What is a treaty?','A formal agreement between parties','A battle','A currency','A law court','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','Which institution commonly makes laws in democracies?','Judiciary','Legislature','Police','Military','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What is a monarchy?','Rule by elected officials','Rule by a king or queen','Rule by judges','Rule by a board','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','Which device is used to record laws?','Constitution','Calendar','Map','Recipe','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What is a revolution?','A gradual change','A rapid fundamental change','A type of music','A type of food','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','Which system separates powers among branches?','Unitary','Federal','Separation of Powers','Monarchy','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What is suffrage?','Right to vote','Tax rule','Judicial power','Trade agreement','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','Who leads an executive branch in many countries?','Prime Minister or President','Judge','Teacher','Doctor','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What is a constitution?','A meal','A foundational legal document','A festival','A sport','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','Which term means rule by the people?','Oligarchy','Democracy','Autocracy','Feudalism','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What is a colony?','A type of government','A territory controlled by another state','A local law','A ceremony','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What does diplomacy primarily involve?','Trade alone','Negotiation between states','Warfare','Sports','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What is an election?','An appointment','A process to choose representatives','A type of currency','A law','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','Which body typically interprets laws?','Legislature','Executive','Judiciary','Police','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What is propaganda?','Neutral information','Biased information to influence opinion','A type of treaty','A court decision','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What does impartial mean?','Fair and unbiased','Biased','Partial','Related to parties','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What is impeachment?','A trial for a president or official','A holiday','A tax','A law','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','Which is a famous ancient writing system?','Cuneiform','HTML','Python','Morse Code','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What is archaeology?','Study of stars','Study of past human activity','Study of plants','Study of numbers','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','Which revolution is associated with 1789?','Industrial','American','French','Russian','C');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What is a civil right?','A personal liberty protected by law','A type of tax','A farming practice','A sport rule','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','Which is a famous ancient monument?','Eiffel Tower','Pyramids','Statue of Liberty','Sydney Opera House','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What is feudalism?','A medieval social system with lords and vassals','A modern law','An economic theory','A language','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','Which term means territorial expansion by a state?','Isolation','Imperialism','Democracy','Federalism','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What is a manifesto?','A list of recipes','A public declaration of policies or aims','A map','A song','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','Which institution handles money and banking policy in many countries?','Central Bank','Local shop','Library','Theatres','A');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','Which process leads to a change in government by citizens?','Coup d''etat','Election','Assassination','Propaganda','B');
INSERT INTO questions (topic, question, option_a, option_b, option_c, option_d, correct_option) VALUES ('History & Politics','What is a census?','A population count','A judicial ruling','A treaty','A ceremony','A');

COMMIT;
